#!/usr/bin/env bash
#
# 05 — Instala MANUALMENTE um build do frontend já pronto (dist/ do Vite).
#
# Não roda npm na VM. O build vem do GitHub Actions (artifact "frontend-dist") ou de
# uma máquina de desenvolvimento com:
#   cd apps/web/plantahub-web && VITE_API_URL=https://api.plantahub.com.br npm ci && npm run build
#   tar -C dist -czf frontend-dist.tar.gz .
#   scp frontend-dist.tar.gz <usuario>@<IP>:/tmp/
#
# Uso: sudo bash deploy/scripts/05-install-frontend.sh /tmp/frontend-dist.tar.gz
#      sudo bash deploy/scripts/05-install-frontend.sh /tmp/frontend-dist.zip
#      sudo bash deploy/scripts/05-install-frontend.sh /tmp/dist/
#
# Permissões finais: diretórios 755, arquivos 644, dono root (o Nginx só lê).

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

SRC="${1:-}"
[ -n "$SRC" ] || die "uso: $0 <dist/ | dist.tar.gz | dist.zip>"
[ -e "$SRC" ] || die "$SRC não existe"
[ -x "$RELEASE_BIN" ] || die "$RELEASE_BIN não instalado; rode 03-setup-directories.sh"

RELEASE_ID="manual-$(date -u +%Y%m%dT%H%M%SZ)"
STAGE="$INCOMING_BASE/$RELEASE_ID"

step "Preparando release $RELEASE_ID"
install -d -o root -g root -m 0750 "$STAGE" "$STAGE/frontend"

case "$SRC" in
  *.tar.gz|*.tgz) tar -xzf "$SRC" -C "$STAGE/frontend" --no-same-owner ;;
  *.zip)          unzip -q "$SRC" -d "$STAGE/frontend" ;;
  *)              [ -d "$SRC" ] || die "$SRC não é diretório, .tar.gz nem .zip"
                  cp -R "$SRC/." "$STAGE/frontend/" ;;
esac

# Aceita também um pacote que contenha a pasta dist/ em vez do conteúdo dela.
if [ ! -f "$STAGE/frontend/index.html" ] && [ -f "$STAGE/frontend/dist/index.html" ]; then
  mv "$STAGE/frontend" "$STAGE/frontend.wrap"
  mv "$STAGE/frontend.wrap/dist" "$STAGE/frontend"
  rm -rf "$STAGE/frontend.wrap"
fi
[ -f "$STAGE/frontend/index.html" ] || die "index.html não encontrado no pacote"

if grep -rqs 'localhost:8080' "$STAGE/frontend/assets/"; then
  die "o bundle referencia localhost:8080: foi compilado sem VITE_API_URL de produção"
fi

printf 'release=%s\nsource=manual\nfile=%s\ninstalled_by=%s\n' \
  "$RELEASE_ID" "$(basename "$SRC")" "${SUDO_USER:-root}" > "$STAGE/RELEASE"
ok "frontend preparado em $STAGE"

step "Ativando"
"$RELEASE_BIN" activate "$RELEASE_ID"

step "Frontend instalado"
"$RELEASE_BIN" status
