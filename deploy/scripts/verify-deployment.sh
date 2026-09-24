#!/usr/bin/env bash
#
# Verificação completa do ambiente PlantaHUB. Somente leitura: não altera nada.
#
# Uso:
#   sudo bash deploy/scripts/verify-deployment.sh                # na VM: tudo
#   bash deploy/scripts/verify-deployment.sh --public-only       # de qualquer máquina:
#                                                                # só os testes pela Internet
#
# Sai com código 1 se qualquer verificação FALHAR (avisos não reprovam).

set -uo pipefail

PUBLIC_ONLY=false
[ "${1:-}" = "--public-only" ] && PUBLIC_ONLY=true

ROOT_DOMAIN=plantahub.com.br
WEB_DOMAIN=www.plantahub.com.br
API_DOMAIN=api.plantahub.com.br
BACKEND_PORT=8080
HEALTH_PATH=/health
DB_NAME=plantahub
APP_USER=plantahub
SSH_PORT=22
if [ -r /etc/plantahub/deploy.conf ]; then
  # shellcheck disable=SC1091
  . /etc/plantahub/deploy.conf
fi

SERVICE=plantahub-backend
ENV_FILE=/opt/plantahub/backend/.env
FRONTEND_ROOT=/var/www/plantahub/frontend

PASS=0; FAILS=0; WARNS=0
if [ -t 1 ]; then G=$'\033[32m'; R=$'\033[31m'; Y=$'\033[33m'; B=$'\033[1m'; N=$'\033[0m'; else G=''; R=''; Y=''; B=''; N=''; fi
pass() { PASS=$((PASS + 1)); printf '  %s✓%s %s\n' "$G" "$N" "$*"; }
fail() { FAILS=$((FAILS + 1)); printf '  %s✗ %s%s\n' "$R" "$*" "$N"; }
warn() { WARNS=$((WARNS + 1)); printf '  %s!%s %s\n' "$Y" "$N" "$*"; }
section() { printf '\n%s%s%s\n' "$B" "$*" "$N"; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# ================================================================== na VM
if [ "$PUBLIC_ONLY" = false ]; then
  [ "$(id -u)" -eq 0 ] || { echo "rode com sudo (ou use --public-only)"; exit 2; }

  section "Sistema"
  USE="$(df -P / | awk 'NR==2 {gsub("%","",$5); print $5}')"
  AVAIL="$(df -Ph / | awk 'NR==2 {print $4}')"
  if [ "$USE" -lt 80 ]; then pass "disco /: ${USE}% usado (${AVAIL} livres)"
  elif [ "$USE" -lt 90 ]; then warn "disco /: ${USE}% usado (${AVAIL} livres) — ZIPs de download usam /tmp"
  else fail "disco /: ${USE}% usado (${AVAIL} livres)"; fi

  MEM_AVAIL="$(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo)"
  MEM_TOTAL="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
  SWAP_TOTAL="$(awk '/SwapTotal/ {print int($2/1024)}' /proc/meminfo)"
  if [ "$MEM_AVAIL" -gt 200 ]; then pass "memória: ${MEM_AVAIL} MB disponíveis de ${MEM_TOTAL} MB (swap ${SWAP_TOTAL} MB)"
  else warn "memória: só ${MEM_AVAIL} MB disponíveis de ${MEM_TOTAL} MB (swap ${SWAP_TOTAL} MB)"; fi

  if java -version 2>&1 | grep -q 'version "21'; then pass "java: $(java -version 2>&1 | head -n 1)"; else fail "java 21 ausente"; fi
  if javac -version 2>&1 | grep -q 'javac 21'; then pass "javac: $(javac -version 2>&1)"; else fail "javac 21 ausente (JRE em vez de JDK?)"; fi

  section "Firewall (UFW)"
  UFW="$(ufw status 2>/dev/null || true)"
  if printf '%s' "$UFW" | grep -q 'Status: active'; then pass "UFW ativo"; else fail "UFW inativo"; fi
  for port in "$SSH_PORT" 80 443; do
    if printf '%s' "$UFW" | grep -Eq "^${port}(/tcp)?[[:space:]]+ALLOW"; then pass "UFW libera $port"; else fail "UFW não libera $port"; fi
  done
  for port in 5432 "$BACKEND_PORT"; do
    if printf '%s' "$UFW" | grep -Eq "^${port}(/tcp)?[[:space:]]+ALLOW"; then fail "UFW libera $port publicamente"; else pass "UFW não libera $port"; fi
  done
  warn "o firewall do PROVEDOR não é visível daqui: confirme 22/80/443 no painel da nuvem"

  section "PostgreSQL"
  if systemctl is-active --quiet postgresql; then pass "postgresql ativo"; else fail "postgresql inativo"; fi
  PG_LISTEN="$(ss -H -tln 2>/dev/null | awk '{print $4}' | grep -E ':5432$' | tr '\n' ' ')"
  if printf '%s' "$PG_LISTEN" | grep -Eq '(^| )(0\.0\.0\.0|\*|\[::\]):5432'; then fail "5432 escuta em todas as interfaces: $PG_LISTEN"
  elif [ -n "$PG_LISTEN" ]; then pass "5432 só em loopback: $PG_LISTEN"
  else fail "nada escutando na 5432"; fi

  PSQL() { (cd / && runuser -u postgres -- psql -d "$DB_NAME" -tA -c "$1" 2>/dev/null); }
  if [ "$(PSQL 'select 1')" = "1" ]; then pass "banco $DB_NAME acessível"; else fail "banco $DB_NAME inacessível"; fi

  FLYWAY="$(PSQL "select coalesce(max(version::int),0) || ' ' || count(*) filter (where not success) from flyway_schema_history where version is not null")"
  if [ -n "$FLYWAY" ]; then
    DB_V="${FLYWAY%% *}"; DB_FAILED="${FLYWAY##* }"
    JAR_V="$(unzip -l /opt/plantahub/backend/app.jar 2>/dev/null | grep -oE 'db/migration/V[0-9]+__' | grep -oE '[0-9]+' | sort -n | tail -n 1)"
    if [ "$DB_FAILED" != "0" ]; then fail "flyway: $DB_FAILED migration(s) com success=false"; else pass "flyway: nenhuma migration falha"; fi
    if [ -n "$JAR_V" ] && [ "$DB_V" = "$JAR_V" ]; then pass "flyway: banco em V$DB_V = última migration do jar (V$JAR_V)"
    elif [ -n "$JAR_V" ]; then fail "flyway: banco em V$DB_V, jar traz até V$JAR_V"
    else warn "flyway: banco em V$DB_V (jar não inspecionado)"; fi
  else
    warn "flyway_schema_history ainda não existe (o backend nunca subiu?)"
  fi

  section "Backend"
  if [ -f "$ENV_FILE" ]; then
    PERM="$(stat -c '%U:%G %a' "$ENV_FILE")"
    if [ "$PERM" = "root:$APP_USER 640" ]; then pass ".env: $PERM"; else fail ".env com $PERM (esperado root:$APP_USER 640)"; fi
    if grep -Eq '=TROQUE' "$ENV_FILE"; then fail ".env ainda tem placeholders TROQUE_*"; else pass ".env sem placeholders"; fi
  else
    fail "$ENV_FILE não existe"
  fi

  if systemctl is-active --quiet "$SERVICE"; then pass "$SERVICE ativo desde $(systemctl show -p ActiveEnterTimestamp --value "$SERVICE")"
  else fail "$SERVICE inativo ($(systemctl is-active "$SERVICE" 2>/dev/null))"; fi
  if systemctl is-enabled --quiet "$SERVICE"; then pass "$SERVICE habilitado no boot"; else warn "$SERVICE não habilitado no boot"; fi

  MAINPID="$(systemctl show -p MainPID --value "$SERVICE" 2>/dev/null || echo 0)"
  if [ "${MAINPID:-0}" != "0" ] && [ -r "/proc/$MAINPID/environ" ]; then
    PROFILE="$(tr '\0' '\n' < "/proc/$MAINPID/environ" | grep '^SPRING_PROFILES_ACTIVE=' | cut -d= -f2-)"
    if [[ ",$PROFILE," == *",prod,"* ]]; then pass "processo rodando com SPRING_PROFILES_ACTIVE=$PROFILE"; else fail "processo sem perfil prod (SPRING_PROFILES_ACTIVE='$PROFILE')"; fi
    RUNAS="$(ps -o user= -p "$MAINPID")"
    if [ "$RUNAS" = "$APP_USER" ]; then pass "processo roda como $RUNAS"; else fail "processo roda como $RUNAS"; fi
  fi

  APP_LISTEN="$(ss -H -tln 2>/dev/null | awk '{print $4}' | grep -E ":${BACKEND_PORT}\$" | tr '\n' ' ')"
  if printf '%s' "$APP_LISTEN" | grep -Eq "^127\.0\.0\.1:${BACKEND_PORT} ?\$"; then pass "porta ${BACKEND_PORT} só em 127.0.0.1"
  elif [ -n "$APP_LISTEN" ]; then fail "porta ${BACKEND_PORT} exposta: $APP_LISTEN"
  else fail "nada escutando na ${BACKEND_PORT}"; fi

  BODY="$(curl -fsS -m 5 "http://127.0.0.1:${BACKEND_PORT}${HEALTH_PATH}" 2>/dev/null || true)"
  if printf '%s' "$BODY" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"'; then pass "health local: $BODY"; else fail "health local: '${BODY:-sem resposta}'"; fi

  if [ -x /usr/local/sbin/plantahub-release ]; then
    printf '%s\n' "$(/usr/local/sbin/plantahub-release status 2>&1 | sed 's/^/      /')"
  fi

  section "Nginx e arquivos do frontend"
  if systemctl is-active --quiet nginx; then pass "nginx ativo"; else fail "nginx inativo"; fi
  if nginx -t >/dev/null 2>&1; then pass "nginx -t OK"; else fail "nginx -t falhou"; fi
  for site in plantahub-api plantahub-frontend; do
    if [ -e "/etc/nginx/sites-enabled/$site" ]; then
      if grep -q 'listen 443' "/etc/nginx/sites-enabled/$site"; then pass "$site habilitado (HTTPS)"; else warn "$site habilitado só em HTTP (SSL pendente)"; fi
    else fail "$site não habilitado"; fi
  done
  if [ -f "$FRONTEND_ROOT/index.html" ]; then
    pass "$FRONTEND_ROOT → $(readlink -f "$FRONTEND_ROOT")"
    ASSET="$(find "$FRONTEND_ROOT/assets/" -maxdepth 1 -name '*.js' -print -quit 2>/dev/null)"
    if [ -n "$ASSET" ] && runuser -u www-data -- test -r "$ASSET"; then pass "www-data lê $(basename "$ASSET")"; else fail "www-data NÃO lê os assets (permissões/travessia)"; fi
    BAD="$(find -L "$FRONTEND_ROOT/" \( -type d ! -perm -0005 \) -o \( -type f ! -perm -0004 \) 2>/dev/null | head -n 3)"
    if [ -z "$BAD" ]; then pass "permissões: diretórios com o+rx, arquivos com o+r"; else fail "sem permissão de leitura: $BAD"; fi
    namei -m "$ASSET" 2>/dev/null | sed 's/^/      /'
  else
    fail "$FRONTEND_ROOT/index.html não existe"
  fi
fi

# ================================================================== pela Internet
section "Pela Internet (DNS real)"

check() {  # check <descrição> <url> <código esperado> [padrão do content-type] [extra curl args...]
  local desc="$1" url="$2" want="$3" want_ct="$4"; shift 4
  local out code ct
  out="$(curl -sS -m 15 -o "$TMP/body" -D "$TMP/headers" -w '%{http_code}|%{content_type}' "$@" "$url" 2>&1)" || true
  code="${out%%|*}"; ct="${out#*|}"
  if [ "$code" = "$want" ] && { [ -z "$want_ct" ] || [[ "$ct" == *$want_ct* ]]; }; then
    pass "$desc → $code ${ct}"
    return 0
  fi
  fail "$desc → '$out' (esperado $want${want_ct:+ e Content-Type ~ $want_ct})"
  return 1
}

check "API health  https://$API_DOMAIN$HEALTH_PATH" "https://$API_DOMAIN$HEALTH_PATH" 200 json \
  && { grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"' "$TMP/body" && pass "corpo: $(cat "$TMP/body")" || fail "corpo inesperado: $(head -c 200 "$TMP/body")"; }

check "API http→https" "http://$API_DOMAIN$HEALTH_PATH" 301 ""

if check "CORS preflight Origin https://$WEB_DOMAIN" "https://$API_DOMAIN/v1/auth/me" 200 "" \
    -X OPTIONS -H "Origin: https://$WEB_DOMAIN" -H 'Access-Control-Request-Method: GET'; then
  if grep -qi "^access-control-allow-origin: https://$WEB_DOMAIN" "$TMP/headers"; then pass "Access-Control-Allow-Origin: https://$WEB_DOMAIN"
  else fail "preflight sem Access-Control-Allow-Origin para https://$WEB_DOMAIN (APP_CORS_ALLOWED_ORIGINS?)"; fi
fi

if check "Frontend  https://$WEB_DOMAIN/" "https://$WEB_DOMAIN/" 200 text/html; then
  JS="$(grep -o 'src="/assets/[^"]*\.js"' "$TMP/body" | head -n 1 | cut -d'"' -f2)"
  if [ -n "$JS" ]; then
    if check "Asset JS  $JS" "https://$WEB_DOMAIN$JS" 200 javascript; then
      head -c 200 "$TMP/body" | grep -qiE '<!doctype|<html' && fail "o asset JS veio com conteúdo HTML" || pass "o asset JS é JavaScript de verdade"
    fi
  else
    fail "index.html não referencia /assets/*.js"
  fi
fi
check "Asset inexistente dá 404 (não index.html)" "https://$WEB_DOMAIN/assets/nao-existe-$$.js" 404 ""
check "Rota SPA /produtos" "https://$WEB_DOMAIN/produtos" 200 text/html

for url in "http://$ROOT_DOMAIN/" "https://$ROOT_DOMAIN/" "http://$WEB_DOMAIN/"; do
  if check "Redirect $url" "$url" 301 ""; then
    LOC="$(grep -i '^location:' "$TMP/headers" | tr -d '\r' | cut -d' ' -f2)"
    if [ "$LOC" = "https://$WEB_DOMAIN/" ]; then pass "  → $LOC"; else fail "  → '$LOC' (esperado https://$WEB_DOMAIN/)"; fi
  fi
done

section "Certificados"
for domain in "$API_DOMAIN" "$ROOT_DOMAIN" "$WEB_DOMAIN"; do
  END="$(echo | openssl s_client -connect "$domain:443" -servername "$domain" 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)"
  if [ -z "$END" ]; then fail "$domain: sem certificado na 443"; continue; fi
  DAYS=$(( ($(date -d "$END" +%s) - $(date +%s)) / 86400 ))
  if [ "$DAYS" -gt 14 ]; then pass "$domain: expira em $DAYS dias ($END)"
  elif [ "$DAYS" -gt 0 ]; then warn "$domain: expira em $DAYS dias — renovação automática falhando?"
  else fail "$domain: certificado expirado"; fi
done

# ================================================================== resumo
printf '\n%sResultado:%s %s%d ok%s · %s%d avisos%s · %s%d falhas%s\n' \
  "$B" "$N" "$G" "$PASS" "$N" "$Y" "$WARNS" "$N" "$R" "$FAILS" "$N"
[ "$FAILS" -eq 0 ]
