#!/usr/bin/env bash
#
# plantahub-release — ativa, verifica e reverte releases do PlantaHUB na VM.
#
# Instalado por 03-setup-directories.sh em /usr/local/sbin/plantahub-release
# (root:root 0755). O usuário "deploy" só pode rodá-lo via sudo (ver sudoers).
#
# Uso:
#   plantahub-release activate <release-id>        ativa o que estiver em
#                                                  /opt/plantahub/incoming/<release-id>/
#                                                    app.jar     → backend
#                                                    frontend/   → frontend (dist/)
#                                                    RELEASE     → metadados (opcional)
#   plantahub-release rollback backend|frontend|all
#   plantahub-release status
#
# Layout:
#   /opt/plantahub/backend/releases/<id>/app.jar
#   /opt/plantahub/backend/current  -> releases/<id>
#   /opt/plantahub/backend/previous -> releases/<id>
#   /opt/plantahub/backend/app.jar  -> current/app.jar      (caminho usado pelo systemd)
#   /var/www/plantahub/releases/<id>/                       (conteúdo de dist/)
#   /var/www/plantahub/frontend     -> releases/<id>        (root do Nginx)
#   /var/www/plantahub/previous     -> releases/<id>
#
# Fluxo do activate: valida pacote → backup do banco → troca atômica do symlink do
# backend → restart → health check → (falhou? volta o symlink, restart, sai com erro)
# → troca atômica do frontend → verificação HTTP real via Nginx (index + asset JS +
# MIME + 404 para asset inexistente) → (falhou? volta o symlink, sai com erro).

set -Eeuo pipefail
umask 022
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export PATH LC_ALL=C

# ------------------------------------------------------------------ configuração

ROOT_DOMAIN=plantahub.com.br
WEB_DOMAIN=www.plantahub.com.br
API_DOMAIN=api.plantahub.com.br
DB_NAME=plantahub
BACKEND_PORT=8080
HEALTH_PATH=/health
HEALTH_TIMEOUT_SECONDS=180
KEEP_RELEASES=5
KEEP_DB_BACKUPS=10
DB_BACKUP_BEFORE_DEPLOY=true

CONF_FILE=/etc/plantahub/deploy.conf
if [ -r "$CONF_FILE" ]; then
  # shellcheck disable=SC1090
  . "$CONF_FILE"
fi

BACKEND_BASE=/opt/plantahub/backend
FRONTEND_BASE=/var/www/plantahub
INCOMING_BASE=/opt/plantahub/incoming
# Área só do root: o pacote é movido para cá ANTES de qualquer validação, para que o
# usuário deploy (dono de incoming/) não consiga trocar arquivos por symlinks entre a
# checagem e a cópia.
STAGING_BASE=/opt/plantahub/staging
DB_BACKUP_DIR=/var/backups/plantahub/db
SERVICE=plantahub-backend
# /run (e não /run/lock, que é 1777): nenhum outro usuário consegue criar o lock antes.
LOCK_FILE=/run/plantahub-release.lock

# ------------------------------------------------------------------ utilidades

log()  { printf '[plantahub-release] %s\n' "$*"; }
fail() { printf '[plantahub-release] ERRO: %s\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || fail "precisa rodar como root (sudo)"

[[ "$DB_NAME" =~ ^[a-z_][a-z0-9_]{0,62}$ ]] || fail "DB_NAME inválido em $CONF_FILE"
[[ "$BACKEND_PORT" =~ ^[0-9]{2,5}$ ]] || fail "BACKEND_PORT inválido em $CONF_FILE"
[[ "$HEALTH_PATH" =~ ^/[A-Za-z0-9/_.-]*$ ]] || fail "HEALTH_PATH inválido em $CONF_FILE"

valid_release_id() {
  [[ "$1" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$ ]] && [[ "$1" != *..* ]]
}

# Alvo relativo de um symlink ("releases/<id>"), ou vazio.
link_target() {
  if [ -L "$1" ]; then readlink "$1"; fi
}

# Troca atômica: cria o novo symlink ao lado e renomeia por cima (rename(2)).
swap_link() {
  local link="$1" target="$2" tmp
  tmp="${link}.tmp.$$"
  ln -sfn "$target" "$tmp"
  mv -Tf "$tmp" "$link"
}

restore_link() {
  local link="$1" target="$2"
  if [ -n "$target" ]; then
    swap_link "$link" "$target"
  else
    rm -f "$link"
  fi
}

# Zera o contador de StartLimitBurst antes de reiniciar: depois de um jar ruim entrar em
# crash-loop, o systemd recusaria o restart do rollback ("start request repeated too
# quickly"). A falha do restart não aborta: quem decide é o health check.
restart_backend() {
  systemctl reset-failed "$SERVICE" 2>/dev/null || true
  systemctl restart "$SERVICE" || log "systemctl restart $SERVICE retornou erro"
}

# ------------------------------------------------------------------ backend

ensure_backend_layout() {
  mkdir -p "$BACKEND_BASE/releases"
  chmod 755 "$BACKEND_BASE" "$BACKEND_BASE/releases"

  # Migração do layout antigo (app.jar como arquivo comum, VM anterior).
  if [ -f "$BACKEND_BASE/app.jar" ] && [ ! -L "$BACKEND_BASE/app.jar" ]; then
    local legacy
    legacy="legacy-$(date -u +%Y%m%dT%H%M%SZ)"
    mkdir -p "$BACKEND_BASE/releases/$legacy"
    mv "$BACKEND_BASE/app.jar" "$BACKEND_BASE/releases/$legacy/app.jar"
    swap_link "$BACKEND_BASE/current" "releases/$legacy"
    log "app.jar antigo preservado em releases/$legacy"
  fi

  if [ "$(link_target "$BACKEND_BASE/app.jar")" != "current/app.jar" ]; then
    swap_link "$BACKEND_BASE/app.jar" "current/app.jar"
  fi
}

# Confere o .env ANTES de trocar a versão: uma variável obrigatória faltando derruba o
# boot, e descobrir isso depois do restart significa downtime. O arquivo é lido como
# texto — nunca executado.
check_env() {
  local env_file="$BACKEND_BASE/.env" key value missing=""
  local required="SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD JWT_SECRET APP_S3_BUCKET APP_S3_REGION INFINITEPAY_HANDLE INFINITEPAY_REDIRECT_URL INFINITEPAY_WEBHOOK_URL"

  [ -f "$env_file" ] || fail "$env_file não existe (rode 03-setup-directories.sh e preencha)"

  env_value() {
    grep -E "^$1=" "$env_file" | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'\$//" || true
  }

  for key in $required; do
    value="$(env_value "$key")"
    if [ -z "$value" ] || [[ "$value" == TROQUE* ]]; then
      missing="$missing $key"
    fi
  done
  [ -z "$missing" ] || fail "variáveis obrigatórias vazias ou com placeholder em $env_file:$missing"

  value="$(env_value JWT_SECRET)"
  [ "${#value}" -ge 32 ] || fail "JWT_SECRET tem ${#value} caracteres; o mínimo é 32 (HMAC-SHA256)"

  value="$(env_value SPRING_PROFILES_ACTIVE)"
  if [ -n "$value" ] && [[ ",$value," != *",prod,"* ]]; then
    fail "SPRING_PROFILES_ACTIVE=$value no .env não inclui 'prod'"
  fi

  if grep -q $'\r' "$env_file"; then
    fail "$env_file tem finais de linha CRLF (Windows); corrija com: sed -i 's/\\r\$//' $env_file"
  fi
}

wait_health() {
  local url="http://127.0.0.1:${BACKEND_PORT}${HEALTH_PATH}"
  local deadline body
  deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))

  log "aguardando $url (até ${HEALTH_TIMEOUT_SECONDS}s)"
  sleep 3

  while [ "$(date +%s)" -lt "$deadline" ]; do
    if systemctl is-failed --quiet "$SERVICE"; then
      log "o serviço $SERVICE entrou em estado failed"
      return 1
    fi

    body="$(curl -fsS -m 5 "$url" 2>/dev/null || true)"

    # Resposta real de HealthController: {"status":"ok"}
    if printf '%s' "$body" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"'; then
      log "health check OK: $body"
      return 0
    fi

    sleep 3
  done

  log "health check não respondeu {\"status\":\"ok\"} dentro do prazo"
  return 1
}

backup_db() {
  local id="$1" exists out

  if [ "$DB_BACKUP_BEFORE_DEPLOY" != "true" ]; then
    log "backup do banco desativado (DB_BACKUP_BEFORE_DEPLOY=$DB_BACKUP_BEFORE_DEPLOY)"
    return 0
  fi

  if ! command -v pg_dump >/dev/null 2>&1; then
    log "pg_dump não encontrado; backup pulado"
    return 0
  fi

  exists="$(cd / && runuser -u postgres -- psql -tAc \
    "select 1 from pg_database where datname = '${DB_NAME}'" 2>/dev/null || true)"

  if [ "$exists" != "1" ]; then
    log "banco ${DB_NAME} ainda não existe; backup pulado"
    return 0
  fi

  mkdir -p "$DB_BACKUP_DIR"
  chmod 700 "$DB_BACKUP_DIR"
  out="$DB_BACKUP_DIR/${DB_NAME}-$(date -u +%Y%m%dT%H%M%SZ)-antes-de-${id}.dump"

  log "pg_dump de ${DB_NAME} → $out"
  (cd / && runuser -u postgres -- pg_dump -Fc "$DB_NAME") > "$out.partial"
  mv "$out.partial" "$out"
  chmod 600 "$out"
}

activate_backend() {
  local id="$1" src="$2"
  local jar="$src/app.jar" rel="$BACKEND_BASE/releases/$id"
  local orig_current orig_previous size

  [ ! -L "$jar" ] && [ -f "$jar" ] || fail "$jar não é um arquivo regular"

  size="$(stat -c %s "$jar")"
  [ "$size" -gt 1000000 ] || fail "$jar tem só $size bytes; não parece o jar do Spring Boot"

  # Filtro do próprio unzip, sem pipe: `| grep -q` sob pipefail pode falhar com SIGPIPE.
  if ! unzip -l "$jar" 'BOOT-INF/classes/*' >/dev/null 2>&1; then
    fail "$jar não é um jar executável do Spring Boot (sem BOOT-INF/classes/)"
  fi

  [ ! -e "$rel" ] || fail "a release $id já existe em $rel"

  check_env
  ensure_backend_layout

  mkdir -p "$rel"
  install -o root -g root -m 0644 "$jar" "$rel/app.jar"
  if [ -f "$src/RELEASE" ] && [ ! -L "$src/RELEASE" ]; then
    head -c 4096 "$src/RELEASE" > "$rel/RELEASE"
    chmod 644 "$rel/RELEASE"
  fi
  chmod 755 "$rel"

  orig_current="$(link_target "$BACKEND_BASE/current")"
  orig_previous="$(link_target "$BACKEND_BASE/previous")"

  # Antes de trocar qualquer coisa: se o dump falhar, nada muda.
  backup_db "$id"

  swap_link "$BACKEND_BASE/current" "releases/$id"
  if [ -n "$orig_current" ]; then
    swap_link "$BACKEND_BASE/previous" "$orig_current"
  fi

  log "backend: current → releases/$id (anterior: ${orig_current:-nenhuma})"
  restart_backend

  if wait_health; then
    log "backend $id no ar"
    return 0
  fi

  log "últimas linhas do journal:"
  journalctl -u "$SERVICE" -n 80 --no-pager 2>/dev/null || true
  touch "$rel/FAILED"

  if [ -z "$orig_current" ]; then
    fail "backend $id não ficou saudável e não há versão anterior para voltar"
  fi

  log "ROLLBACK automático do backend para $orig_current"
  restore_link "$BACKEND_BASE/current" "$orig_current"
  restore_link "$BACKEND_BASE/previous" "$orig_previous"
  restart_backend

  if wait_health; then
    fail "backend $id reprovado no health check; versão anterior ($orig_current) restaurada e saudável"
  fi

  fail "backend $id reprovado E o rollback para $orig_current também não respondeu — intervenção manual"
}

# ------------------------------------------------------------------ frontend

ensure_frontend_layout() {
  mkdir -p "$FRONTEND_BASE/releases" "$FRONTEND_BASE/manifests"
  chmod 755 /var/www "$FRONTEND_BASE" "$FRONTEND_BASE/releases"
  chmod 755 "$FRONTEND_BASE/manifests"

  # Migração do layout antigo (frontend/ como diretório comum, VM anterior).
  if [ -d "$FRONTEND_BASE/frontend" ] && [ ! -L "$FRONTEND_BASE/frontend" ]; then
    local legacy
    legacy="legacy-$(date -u +%Y%m%dT%H%M%SZ)"
    mv "$FRONTEND_BASE/frontend" "$FRONTEND_BASE/releases/$legacy"
    swap_link "$FRONTEND_BASE/frontend" "releases/$legacy"
    log "frontend antigo preservado em releases/$legacy"
  fi
}

first_js_of() {
  grep -o 'src="/assets/[^"]*\.js"' "$1" | head -n 1 | cut -d'"' -f2 || true
}

verify_frontend() {
  local expected_js="$1"
  local scheme port base resolve tmp result code ctype js

  if ! systemctl is-active --quiet nginx; then
    log "nginx inativo: verificação HTTP do frontend pulada"
    return 0
  fi

  if [ ! -e /etc/nginx/sites-enabled/plantahub-frontend ]; then
    log "site plantahub-frontend não habilitado: verificação HTTP pulada"
    return 0
  fi

  if grep -q 'listen 443' /etc/nginx/sites-enabled/plantahub-frontend \
      && [ -f "/etc/letsencrypt/live/${ROOT_DOMAIN}/fullchain.pem" ]; then
    scheme=https; port=443
  else
    scheme=http; port=80
  fi

  base="${scheme}://${WEB_DOMAIN}"
  resolve="${WEB_DOMAIN}:${port}:127.0.0.1"
  tmp="$(mktemp -d)"

  result="$(curl -sS -m 10 --resolve "$resolve" -o "$tmp/index.html" \
    -w '%{http_code}|%{content_type}' "$base/" 2>&1 || true)"
  code="${result%%|*}"; ctype="${result#*|}"
  if [ "$code" != "200" ] || [[ "$ctype" != text/html* ]]; then
    log "GET $base/ → $result (esperado 200 text/html)"
    rm -rf "$tmp"; return 1
  fi

  js="$(first_js_of "$tmp/index.html")"
  if [ "$js" != "$expected_js" ]; then
    log "o index.html servido aponta para '$js', esperado '$expected_js' (release errada no ar?)"
    rm -rf "$tmp"; return 1
  fi

  result="$(curl -sS -m 10 --resolve "$resolve" -o "$tmp/app.js" \
    -w '%{http_code}|%{content_type}' "$base$js" 2>&1 || true)"
  code="${result%%|*}"; ctype="${result#*|}"
  if [ "$code" != "200" ] || [[ "$ctype" != *javascript* ]]; then
    log "GET $base$js → $result (esperado 200 com Content-Type JavaScript)"
    rm -rf "$tmp"; return 1
  fi
  if head -c 200 "$tmp/app.js" | grep -qiE '<!doctype|<html'; then
    log "GET $base$js devolveu HTML no lugar de JavaScript"
    rm -rf "$tmp"; return 1
  fi

  code="$(curl -s -m 10 --resolve "$resolve" -o /dev/null -w '%{http_code}' \
    "$base/assets/plantahub-verificacao-inexistente-$$.js" || true)"
  if [ "$code" != "404" ]; then
    log "asset inexistente respondeu $code (esperado 404 — o fallback da SPA está capturando /assets/)"
    rm -rf "$tmp"; return 1
  fi

  rm -rf "$tmp"
  log "frontend verificado via Nginx: $base/ (200 html), $js (200 javascript), asset inexistente 404"
  return 0
}

activate_frontend() {
  local id="$1" src="$2"
  local dir="$src/frontend" rel="$FRONTEND_BASE/releases/$id"
  local orig_current orig_previous expected_js prev_id manifest f carried=0

  [ -d "$dir" ] && [ ! -L "$dir" ] || fail "$dir não é um diretório"
  [ -f "$dir/index.html" ] || fail "$dir/index.html não existe — isto é mesmo o dist/ do Vite?"

  if [ -n "$(find "$dir" -type l -print -quit)" ]; then
    fail "o pacote do frontend contém symlinks; recusado"
  fi
  if [ -n "$(find "$dir" ! -type f ! -type d -print -quit)" ]; then
    fail "o pacote do frontend contém arquivos especiais; recusado"
  fi

  expected_js="$(first_js_of "$dir/index.html")"
  [ -n "$expected_js" ] || fail "index.html não referencia nenhum /assets/*.js"
  [ -f "$dir$expected_js" ] || fail "index.html aponta para $expected_js, que não está no pacote"

  [ ! -e "$rel" ] || fail "a release $id já existe em $rel"

  ensure_frontend_layout

  mkdir -p "$rel"
  cp -R "$dir/." "$rel/"

  # Lista dos assets que ESTA release trouxe (fora da raiz servida pelo Nginx).
  find "$rel/assets" -maxdepth 1 -type f -printf '%f\n' | sort > "$FRONTEND_BASE/manifests/$id.assets"

  orig_current="$(link_target "$FRONTEND_BASE/frontend")"
  orig_previous="$(link_target "$FRONTEND_BASE/previous")"

  # Carrega os assets da release anterior: quem está com o index.html antigo aberto
  # ainda pede os chunks antigos (ex.: AdminApp-*.js, carregado sob demanda). Sem isto,
  # esses pedidos dão 404 e a tela quebra até o usuário recarregar.
  if [ -n "$orig_current" ] && [ -d "$FRONTEND_BASE/$orig_current/assets" ]; then
    prev_id="$(basename "$orig_current")"
    manifest="$FRONTEND_BASE/manifests/$prev_id.assets"
    if [ ! -f "$manifest" ]; then
      # Release sem manifesto (legada/manual antiga): gera e guarda um, para não
      # carregar assets em cascata na próxima vez.
      find "$FRONTEND_BASE/$orig_current/assets" -maxdepth 1 -type f -printf '%f\n' | sort > "$manifest"
    fi
    while IFS= read -r f; do
      if [[ "$f" =~ ^[A-Za-z0-9._-]+$ ]] \
          && [ -f "$FRONTEND_BASE/$orig_current/assets/$f" ] \
          && [ ! -e "$rel/assets/$f" ]; then
        cp -p "$FRONTEND_BASE/$orig_current/assets/$f" "$rel/assets/$f"
        carried=$((carried + 1))
      fi
    done < "$manifest"
    log "frontend: $carried asset(s) da release anterior mantidos para abas abertas"
  fi

  # Permissões: o Nginx (www-data) precisa ler arquivos e atravessar diretórios.
  chown -R root:root "$rel"
  find "$rel" -type d -exec chmod 755 {} +
  find "$rel" -type f -exec chmod 644 {} +

  # nginx -t ANTES da troca: um erro de configuração não pode deixar a versão nova no
  # ar sem verificação. (A troca de symlink em si não exige reload.)
  local nginx_ok=false
  if systemctl is-active --quiet nginx; then
    nginx -t -q || fail "nginx -t falhou; frontend $id não foi ativado"
    nginx_ok=true
  fi

  swap_link "$FRONTEND_BASE/frontend" "releases/$id"
  if [ -n "$orig_current" ]; then
    swap_link "$FRONTEND_BASE/previous" "$orig_current"
  fi
  log "frontend: frontend → releases/$id (anterior: ${orig_current:-nenhuma})"

  if [ "$nginx_ok" = true ]; then
    systemctl reload nginx || log "reload do nginx retornou erro"
  fi

  if verify_frontend "$expected_js"; then
    log "frontend $id no ar"
    return 0
  fi

  touch "$FRONTEND_BASE/manifests/$id.FAILED"

  if [ -z "$orig_current" ]; then
    fail "frontend $id reprovado na verificação e não há versão anterior para voltar"
  fi

  log "ROLLBACK automático do frontend para $orig_current"
  restore_link "$FRONTEND_BASE/frontend" "$orig_current"
  restore_link "$FRONTEND_BASE/previous" "$orig_previous"
  fail "frontend $id reprovado na verificação; versão anterior ($orig_current) restaurada"
}

# ------------------------------------------------------------------ limpeza

prune_releases() {
  local base="$1" current_link="$2" previous_link="$3" keep="$4" manifests="${5:-}"
  local cur prev d

  [ -d "$base" ] || return 0
  cur="$(basename "$(link_target "$current_link")" 2>/dev/null || true)"
  prev="$(basename "$(link_target "$previous_link")" 2>/dev/null || true)"

  # shellcheck disable=SC2012
  ls -1t "$base" | tail -n +"$((keep + 1))" | while IFS= read -r d; do
    if [ -n "$d" ] && [ "$d" != "$cur" ] && [ "$d" != "$prev" ]; then
      rm -rf -- "${base:?}/$d"
      if [ -n "$manifests" ]; then
        rm -f -- "$manifests/$d.assets" "$manifests/$d.FAILED"
      fi
      log "release antiga removida: $base/$d"
    fi
  done
}

prune_all() {
  prune_releases "$BACKEND_BASE/releases" "$BACKEND_BASE/current" "$BACKEND_BASE/previous" "$KEEP_RELEASES"
  prune_releases "$FRONTEND_BASE/releases" "$FRONTEND_BASE/frontend" "$FRONTEND_BASE/previous" "$KEEP_RELEASES"     "$FRONTEND_BASE/manifests"

  if [ -d "$DB_BACKUP_DIR" ]; then
    # shellcheck disable=SC2012
    { ls -1t "$DB_BACKUP_DIR"/*.dump 2>/dev/null || true; } \
      | tail -n +"$((KEEP_DB_BACKUPS + 1))" | xargs -r rm -f --
  fi

  local dir
  for dir in "$INCOMING_BASE" "$STAGING_BASE"; do
    if [ -d "$dir" ]; then
      find "$dir" -mindepth 1 -maxdepth 1 -mtime +3 -exec rm -rf -- {} +
    fi
  done
}

# ------------------------------------------------------------------ comandos

cmd_activate() {
  local id="${1:-}" src

  valid_release_id "$id" || fail "release-id inválido: '$id'"
  [ -d "$INCOMING_BASE/$id" ] && [ ! -L "$INCOMING_BASE/$id" ] || fail "$INCOMING_BASE/$id não existe"

  # Tira o pacote do alcance do usuário deploy antes de olhar para ele: mv -T não segue
  # symlinks e é um rename no mesmo filesystem; em seguida o dono passa a ser root e só
  # então as validações e cópias acontecem.
  install -d -o root -g root -m 0700 "$STAGING_BASE"
  src="$STAGING_BASE/$id"
  [ ! -e "$src" ] || fail "$src já existe (release repetida?)"
  mv -T -- "$INCOMING_BASE/$id" "$src"
  [ -d "$src" ] && [ ! -L "$src" ] || fail "$src não é um diretório"
  chown -R root:root "$src"
  chmod -R go-w "$src"
  if [ -n "$(find "$src" -type l -print -quit)" ]; then
    fail "o pacote contém symlinks; recusado"
  fi
  if [ -n "$(find "$src" ! -type f ! -type d -print -quit)" ]; then
    fail "o pacote contém arquivos especiais (FIFO, device, socket); recusado"
  fi

  local has_backend=false has_frontend=false
  [ -e "$src/app.jar" ] && has_backend=true
  [ -e "$src/frontend" ] && has_frontend=true
  if [ "$has_backend" = false ] && [ "$has_frontend" = false ]; then
    fail "$src não contém app.jar nem frontend/"
  fi

  log "ativando release $id (backend=$has_backend, frontend=$has_frontend)"

  # Backend primeiro: um frontend novo pode depender de endpoint novo. Se o backend
  # falhar, o script sai aqui e o frontend nem é tocado.
  if [ "$has_backend" = true ]; then
    activate_backend "$id" "$src"
  fi
  if [ "$has_frontend" = true ]; then
    activate_frontend "$id" "$src"
  fi

  rm -rf -- "${src:?}"
  prune_all
  log "release $id ativada com sucesso"
}

cmd_rollback() {
  local what="${1:-}" cur prev

  case "$what" in
    backend|all)
      cur="$(link_target "$BACKEND_BASE/current")"
      prev="$(link_target "$BACKEND_BASE/previous")"
      [ -n "$prev" ] && [ -f "$BACKEND_BASE/$prev/app.jar" ] || fail "não há release anterior do backend"
      swap_link "$BACKEND_BASE/current" "$prev"
      swap_link "$BACKEND_BASE/previous" "$cur"
      log "backend: current → $prev (a que estava no ar, $cur, virou previous)"
      restart_backend
      wait_health || fail "backend não ficou saudável após o rollback; veja journalctl -u $SERVICE"
      ;;
  esac

  case "$what" in
    frontend|all)
      cur="$(link_target "$FRONTEND_BASE/frontend")"
      prev="$(link_target "$FRONTEND_BASE/previous")"
      [ -n "$prev" ] && [ -f "$FRONTEND_BASE/$prev/index.html" ] || fail "não há release anterior do frontend"
      swap_link "$FRONTEND_BASE/frontend" "$prev"
      swap_link "$FRONTEND_BASE/previous" "$cur"
      log "frontend: frontend → $prev (a que estava no ar, $cur, virou previous)"
      verify_frontend "$(first_js_of "$FRONTEND_BASE/$prev/index.html")" \
        || fail "frontend restaurado, mas a verificação HTTP falhou"
      ;;
  esac

  case "$what" in
    backend|frontend|all) log "rollback ($what) concluído" ;;
    *) fail "uso: plantahub-release rollback backend|frontend|all" ;;
  esac
}

cmd_status() {
  printf 'backend  current : %s\n' "$(link_target "$BACKEND_BASE/current")"
  printf 'backend  previous: %s\n' "$(link_target "$BACKEND_BASE/previous")"
  printf 'frontend current : %s\n' "$(link_target "$FRONTEND_BASE/frontend")"
  printf 'frontend previous: %s\n' "$(link_target "$FRONTEND_BASE/previous")"
  printf 'serviço          : %s\n' "$(systemctl is-active "$SERVICE" 2>/dev/null || true)"
  printf 'health           : %s\n' \
    "$(curl -fsS -m 5 "http://127.0.0.1:${BACKEND_PORT}${HEALTH_PATH}" 2>/dev/null || echo 'sem resposta')"
}

main() {
  local cmd="${1:-}"
  shift || true

  exec 9>"$LOCK_FILE"
  flock -n 9 || fail "outro plantahub-release já está em execução"

  case "$cmd" in
    activate) cmd_activate "$@" ;;
    rollback) cmd_rollback "$@" ;;
    status)   cmd_status ;;
    *)        sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'; exit 2 ;;
  esac
}

main "$@"
