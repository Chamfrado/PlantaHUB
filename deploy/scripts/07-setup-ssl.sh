#!/usr/bin/env bash
#
# 07 — Emite os certificados Let's Encrypt e ativa a configuração HTTPS definitiva.
#
# Uso:
#   sudo bash deploy/scripts/07-setup-ssl.sh api        # api.plantahub.com.br
#   sudo bash deploy/scripts/07-setup-ssl.sh frontend   # plantahub.com.br + www
#
# O Certbot SÓ roda depois de:
#   1. DNS: cada domínio resolve (A) exatamente para PUBLIC_IP em 1.1.1.1, 8.8.8.8 e
#      nos nameservers autoritativos; AAAA ausente ou apontando para esta VM;
#   2. Porta 80: um arquivo de teste no webroot ACME responde pelo domínio;
#   3. Ensaio: certbot --dry-run (staging, não conta no limite do Let's Encrypt).
#
# Variáveis: ASSUME_EXTERNAL_OK=1 pula a confirmação interativa do teste externo.

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

TARGET="${1:-}"
case "$TARGET" in
  api)
    CERT_NAME="$API_DOMAIN"
    DOMAINS=("$API_DOMAIN")
    SITE=plantahub-api
    ;;
  frontend)
    CERT_NAME="$ROOT_DOMAIN"
    DOMAINS=("$ROOT_DOMAIN" "$WEB_DOMAIN")
    SITE=plantahub-frontend
    ;;
  *) die "uso: $0 api|frontend" ;;
esac

[ -n "$PUBLIC_IP" ] || die "defina PUBLIC_IP em $CONF_FILE"
[ -n "$LETSENCRYPT_EMAIL" ] || die "defina LETSENCRYPT_EMAIL em $CONF_FILE"
command -v dig >/dev/null || die "dig não encontrado (pacote dnsutils)"
command -v certbot >/dev/null || die "certbot não encontrado"
[ -f "/etc/nginx/sites-enabled/$SITE" ] || die "site $SITE não habilitado; rode 06-setup-nginx.sh"

# ------------------------------------------------------------------ 0. IP da VM

step "IP público desta VM"
DETECTED_IP="$(curl -4 -fsS -m 10 https://api.ipify.org 2>/dev/null || true)"
info "configurado: $PUBLIC_IP · detectado pela Internet: ${DETECTED_IP:-não foi possível detectar}"
if [ -n "$DETECTED_IP" ] && [ "$DETECTED_IP" != "$PUBLIC_IP" ]; then
  warn "PUBLIC_IP ($PUBLIC_IP) difere do IP de saída detectado ($DETECTED_IP)."
  warn "Isso é normal se a VM tiver IP de saída diferente (NAT); do contrário, corrija $CONF_FILE."
fi

# ------------------------------------------------------------------ 1. DNS

step "1/4 DNS"
DNS_FAIL=0
AUTH_NS="$(dig +short NS "$ROOT_DOMAIN" @1.1.1.1 2>/dev/null | sed 's/\.$//' | sort -u || true)"
[ -n "$AUTH_NS" ] || warn "não foi possível descobrir os nameservers de $ROOT_DOMAIN"

VM_IPV6="$(ip -6 addr show scope global 2>/dev/null | awk '/inet6/ {print $2}' | cut -d/ -f1 || true)"

for domain in "${DOMAINS[@]}"; do
  for resolver in 1.1.1.1 8.8.8.8 $AUTH_NS; do
    ANSWER="$(dig +short A "$domain" @"$resolver" 2>/dev/null \
      | grep -E '^([0-9]{1,3}\.){3}[0-9]{1,3}$' | sort -u | tr '\n' ' ' | sed 's/ $//' || true)"
    if [ "$ANSWER" = "$PUBLIC_IP" ]; then
      ok "$domain A → $ANSWER  (@$resolver)"
    else
      warn "$domain A → '${ANSWER:-nada}'  (@$resolver) — esperado exatamente $PUBLIC_IP"
      DNS_FAIL=1
    fi
  done

  AAAA="$(dig +short AAAA "$domain" @1.1.1.1 2>/dev/null | grep ':' || true)"
  if [ -n "$AAAA" ]; then
    for v6 in $AAAA; do
      if printf '%s\n' "$VM_IPV6" | grep -qx "$v6"; then
        ok "$domain AAAA → $v6 (é desta VM)"
      else
        warn "$domain AAAA → $v6 NÃO é desta VM. O Let's Encrypt prefere IPv6 e vai falhar: remova o AAAA."
        DNS_FAIL=1
      fi
    done
  fi
done

# Registro criado com o nome completo dentro da zona (erro já visto antes).
BOGUS="${ROOT_DOMAIN}.${ROOT_DOMAIN}"
if [ -n "$(dig +short "$BOGUS" @1.1.1.1 2>/dev/null)" ]; then
  warn "existe um registro $BOGUS: provavelmente '$ROOT_DOMAIN' foi digitado no campo Nome. Apague-o."
fi

[ "$DNS_FAIL" = 0 ] || die "DNS ainda não aponta para $PUBLIC_IP. Corrija/aguarde a propagação e rode de novo."

# ------------------------------------------------------------------ 2. porta 80

step "2/4 Porta 80 alcançável pelo domínio"
TOKEN="plantahub-probe-$(openssl rand -hex 8)"
PROBE="$ACME_WEBROOT/.well-known/acme-challenge/$TOKEN"
install -d -m 0755 "$ACME_WEBROOT/.well-known/acme-challenge"
printf 'ok\n' > "$PROBE"
chmod 644 "$PROBE"
trap 'rm -f "$PROBE"' EXIT

for domain in "${DOMAINS[@]}"; do
  URL="http://$domain/.well-known/acme-challenge/$TOKEN"
  CODE="$(curl -s -o /dev/null -m 10 -w '%{http_code}' --resolve "$domain:80:127.0.0.1" "$URL" || true)"
  [ "$CODE" = 200 ] || die "Nginx local não serve o webroot ACME para $domain (HTTP $CODE)"
  ok "Nginx local serve o ACME de $domain"

  CODE="$(curl -s -o /dev/null -m 10 -w '%{http_code}' --resolve "$domain:80:$PUBLIC_IP" "$URL" || true)"
  if [ "$CODE" = 200 ]; then
    ok "pelo IP público $PUBLIC_IP: 200"
  else
    warn "pelo IP público $PUBLIC_IP: '$CODE' (muitas nuvens não permitem a VM acessar o próprio IP público; não é conclusivo)"
  fi
done

if [ "${ASSUME_EXTERNAL_OK:-0}" != "1" ]; then
  printf '\n%sTeste EXTERNO (obrigatório — valida o firewall do provedor).%s\n' "$C_BLD" "$C_RST"
  printf 'Do SEU computador, fora da VM, rode:\n\n'
  for domain in "${DOMAINS[@]}"; do
    printf '    curl -i http://%s/.well-known/acme-challenge/%s\n' "$domain" "$TOKEN"
  done
  printf '\nEsperado: "HTTP/1.1 200 OK" e o corpo "ok". Timeout = porta 80 bloqueada no provedor.\n'
  printf 'Para a 443 (ainda sem certificado): nc -vz %s 443  →  "refused" é OK; timeout = bloqueada.\n\n' "$PUBLIC_IP"
  read -r -p "Os testes externos passaram? [s/N] " REPLY
  [[ "$REPLY" =~ ^[sSyY]$ ]] || die "libere 80/443 no firewall do provedor e rode de novo"
fi

# ------------------------------------------------------------------ 3. certbot

CERTBOT_ARGS=(certonly --webroot -w "$ACME_WEBROOT" --cert-name "$CERT_NAME"
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email --non-interactive
  --keep-until-expiring)
for domain in "${DOMAINS[@]}"; do CERTBOT_ARGS+=(-d "$domain"); done

step "3/4 Certbot — ensaio (staging)"
certbot "${CERTBOT_ARGS[@]}" --dry-run
ok "ensaio aprovado"

step "3/4 Certbot — emissão real"
certbot "${CERTBOT_ARGS[@]}"
[ -f "/etc/letsencrypt/live/$CERT_NAME/fullchain.pem" ] || die "certificado não encontrado após emissão"
ok "certificado em /etc/letsencrypt/live/$CERT_NAME/"

install -d -m 0755 /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/plantahub-reload-nginx.sh <<'EOF'
#!/bin/sh
# Recarrega o Nginx quando o certbot renova algum certificado.
nginx -t -q && systemctl reload nginx
EOF
chmod 755 /etc/letsencrypt/renewal-hooks/deploy/plantahub-reload-nginx.sh
ok "hook de renovação instalado"

# ------------------------------------------------------------------ 4. HTTPS

step "4/4 Configuração HTTPS definitiva ($SITE)"
BACKUP_CONF="$(mktemp)"
cp "/etc/nginx/sites-available/$SITE" "$BACKUP_CONF"
install -o root -g root -m 0644 "$DEPLOY_DIR/nginx/$SITE.conf" "/etc/nginx/sites-available/$SITE"
if ! nginx -t; then
  cp "$BACKUP_CONF" "/etc/nginx/sites-available/$SITE"
  nginx -t && systemctl reload nginx
  die "a configuração SSL não passou no nginx -t; a versão anterior foi restaurada"
fi
rm -f "$BACKUP_CONF"
systemctl reload nginx
ok "$SITE em HTTPS"

for domain in "${DOMAINS[@]}"; do
  RESULT="$(curl -sS -o /dev/null -m 10 -w '%{http_code} %{redirect_url}' --resolve "$domain:443:127.0.0.1" "https://$domain/" || true)"
  ok "https://$domain/ → $RESULT"
done
if [ "$TARGET" = api ]; then
  RESULT="$(curl -sS -m 10 --resolve "$API_DOMAIN:443:127.0.0.1" "https://$API_DOMAIN$HEALTH_PATH" || true)"
  info "https://$API_DOMAIN$HEALTH_PATH → ${RESULT:-sem resposta (o backend está rodando?)}"
fi

step "Renovação automática"
systemctl is-enabled --quiet certbot.timer && ok "certbot.timer habilitado" || warn "certbot.timer não habilitado"
certbot renew --dry-run --cert-name "$CERT_NAME"
ok "certbot renew --dry-run aprovado"

step "SSL de $TARGET pronto"
if [ "$TARGET" = api ]; then
  info "Próximo: frontend (fase 9) e depois: sudo bash $DEPLOY_DIR/scripts/07-setup-ssl.sh frontend"
else
  info "Próximo: sudo bash $DEPLOY_DIR/scripts/verify-deployment.sh"
fi
