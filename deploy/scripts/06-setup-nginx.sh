#!/usr/bin/env bash
#
# 06 — Instala a configuração do Nginx. Idempotente.
#
# Sem certificado ainda: instala as versões HTTP provisórias (*.http.conf), que servem
# o desafio ACME e permitem testar API e site pela porta 80.
# Com certificado já emitido: instala as versões definitivas (SSL). Nunca rebaixa um
# site que já está em HTTPS.
#
# Uso: sudo bash deploy/scripts/06-setup-nginx.sh

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

command -v nginx >/dev/null || die "nginx não instalado; rode 01-bootstrap-server.sh"

step "Snippets e configuração global"
install -d -m 0755 /etc/nginx/snippets
for f in "$DEPLOY_DIR"/nginx/snippets/*.conf; do
  install -o root -g root -m 0644 "$f" "/etc/nginx/snippets/$(basename "$f")"
  ok "/etc/nginx/snippets/$(basename "$f")"
done
install -o root -g root -m 0644 "$DEPLOY_DIR/nginx/conf.d/plantahub-global.conf" /etc/nginx/conf.d/plantahub-global.conf
ok "/etc/nginx/conf.d/plantahub-global.conf"

if grep -Eq '^[[:space:]]*gzip_types' /etc/nginx/nginx.conf; then
  warn "nginx.conf já define gzip_types; removendo a linha duplicada do plantahub-global.conf"
  sed -i '/^gzip_types /d' /etc/nginx/conf.d/plantahub-global.conf
fi

# MIME: é o mime.types que faz .js sair como JavaScript e não text/plain/html.
grep -Eq '(application|text)/javascript[[:space:]]+js;' /etc/nginx/mime.types \
  || die "/etc/nginx/mime.types não mapeia .js para JavaScript"
grep -Eq '^[[:space:]]*include[[:space:]]+/etc/nginx/mime.types;' /etc/nginx/nginx.conf \
  || die "nginx.conf não inclui /etc/nginx/mime.types"
ok "mime.types mapeia .js → $(grep -Eo '(application|text)/javascript' /etc/nginx/mime.types | head -n 1)"

step "Site padrão"
if [ -L /etc/nginx/sites-enabled/default ]; then
  rm -f /etc/nginx/sites-enabled/default
  ok "site 'default' do Ubuntu desabilitado (o arquivo em sites-available foi mantido)"
fi
install -o root -g root -m 0644 "$DEPLOY_DIR/nginx/plantahub-default.conf" /etc/nginx/sites-available/plantahub-default
ln -sfn /etc/nginx/sites-available/plantahub-default /etc/nginx/sites-enabled/plantahub-default
ok "plantahub-default: Host desconhecido → 444"

install_site() {
  local name="$1" cert_dir="$2" source
  if [ -f "$cert_dir/fullchain.pem" ]; then
    source="$DEPLOY_DIR/nginx/${name}.conf"
    info "$name: certificado encontrado em $cert_dir → versão SSL definitiva"
  else
    source="$DEPLOY_DIR/nginx/${name}.http.conf"
    info "$name: sem certificado ainda → versão HTTP provisória"
  fi
  install -o root -g root -m 0644 "$source" "/etc/nginx/sites-available/$name"
  ln -sfn "/etc/nginx/sites-available/$name" "/etc/nginx/sites-enabled/$name"
  ok "/etc/nginx/sites-available/$name ← $(basename "$source")"
}

step "Sites do PlantaHUB"
install_site plantahub-api "/etc/letsencrypt/live/${API_DOMAIN}"
install_site plantahub-frontend "/etc/letsencrypt/live/${ROOT_DOMAIN}"

step "Validação"
nginx -t
systemctl reload nginx
ok "nginx recarregado"

# Testes locais pela porta 80, com o Host certo (não dependem de DNS).
if curl -fsS -m 5 "http://127.0.0.1:${BACKEND_PORT}${HEALTH_PATH}" >/dev/null 2>&1; then
  CODE="$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H "Host: ${API_DOMAIN}" "http://127.0.0.1${HEALTH_PATH}")"
  case "$CODE" in
    200) ok "API via Nginx (porta 80, Host ${API_DOMAIN}): $CODE" ;;
    301) ok "API via Nginx redireciona para HTTPS (301) — site já definitivo" ;;
    *)   warn "API via Nginx respondeu $CODE" ;;
  esac
else
  info "backend ainda não está rodando: teste da API via Nginx pulado"
fi

if [ -f "$FRONTEND_ROOT/index.html" ]; then
  CODE="$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H "Host: ${WEB_DOMAIN}" http://127.0.0.1/)"
  ok "frontend via Nginx (porta 80, Host ${WEB_DOMAIN}): $CODE"
else
  info "frontend ainda não instalado: teste pulado"
fi

step "Nginx pronto"
info "Próximo: configure o DNS (README, fase 7) e depois: sudo bash $DEPLOY_DIR/scripts/07-setup-ssl.sh api"
