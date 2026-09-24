#!/usr/bin/env bash
#
# 04 — Instala MANUALMENTE um jar já compilado (primeira instalação ou emergência).
#
# Não compila nada: o jar vem do GitHub Actions. Para obtê-lo sem deploy automático:
#   GitHub → Actions → "Deploy PlantaHUB Production" → execução → Artifacts →
#   "backend-jar" (zip com app.jar). Copie para a VM:
#     scp -P 22 app.jar <usuario>@<IP>:/tmp/app.jar
#
# Uso: sudo bash deploy/scripts/04-install-backend.sh /tmp/app.jar
#
# Usa exatamente o mesmo caminho do deploy automático (plantahub-release activate):
# valida o .env, faz pg_dump, troca o symlink, reinicia, espera o health check e
# volta para a versão anterior se falhar.

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

JAR="${1:-}"
[ -n "$JAR" ] || die "uso: $0 /caminho/para/app.jar"
[ -f "$JAR" ] || die "$JAR não existe"
[ -x "$RELEASE_BIN" ] || die "$RELEASE_BIN não instalado; rode 03-setup-directories.sh"

RELEASE_ID="manual-$(date -u +%Y%m%dT%H%M%SZ)"
STAGE="$INCOMING_BASE/$RELEASE_ID"

step "Preparando release $RELEASE_ID"
install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 0750 "$STAGE"
install -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 0640 "$JAR" "$STAGE/app.jar"
printf 'release=%s\nsource=manual\nfile=%s\nsha256=%s\ninstalled_by=%s\n' \
  "$RELEASE_ID" "$(basename "$JAR")" "$(sha256sum "$JAR" | cut -d' ' -f1)" "${SUDO_USER:-root}" \
  > "$STAGE/RELEASE"
ok "jar copiado para $STAGE"

step "Ativando"
"$RELEASE_BIN" activate "$RELEASE_ID"

step "Backend instalado"
"$RELEASE_BIN" status
info "Logs: journalctl -u $SERVICE_NAME -f"
