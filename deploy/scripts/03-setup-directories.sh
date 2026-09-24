#!/usr/bin/env bash
#
# 03 — Diretórios, permissões, .env, unit do systemd, script de release e sudoers.
# Idempotente: nunca sobrescreve um .env existente.
#
# Uso: sudo bash deploy/scripts/03-setup-directories.sh
#
# Resultado:
#   /opt/plantahub/                       root:root        755
#   /opt/plantahub/backend/               root:root        755
#   /opt/plantahub/backend/.env           root:plantahub   640   (segredos de runtime)
#   /opt/plantahub/backend/releases/      root:root        755
#   /opt/plantahub/incoming/              deploy:deploy    750   (upload do CI)
#   /opt/plantahub/staging/               root:root        700   (pacote movido p/ cá antes de validar)
#   /var/www/plantahub/{releases,manifests}/ root:root     755
#   /var/www/letsencrypt/                 root:root        755   (webroot ACME)
#   /var/backups/plantahub/db/            root:root        700   (pg_dump pré-deploy)
#   /usr/local/sbin/plantahub-release     root:root        755
#   /etc/sudoers.d/plantahub-deploy       root:root        440
#   /etc/systemd/system/plantahub-backend.service

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

id "$APP_USER" >/dev/null 2>&1 || die "usuário $APP_USER não existe; rode 01-bootstrap-server.sh"
id "$DEPLOY_USER" >/dev/null 2>&1 || die "usuário $DEPLOY_USER não existe; rode 01-bootstrap-server.sh"

# ------------------------------------------------------------------ diretórios

step "Diretórios"
install -d -o root -g root -m 0755 /opt/plantahub "$BACKEND_BASE" "$BACKEND_BASE/releases"
install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 0750 "$INCOMING_BASE"
install -d -o root -g root -m 0700 /opt/plantahub/staging
install -d -o root -g root -m 0755 /var/www "$FRONTEND_BASE" \
  "$FRONTEND_BASE/releases" "$FRONTEND_BASE/manifests"
install -d -o root -g root -m 0755 "$ACME_WEBROOT" \
  "$ACME_WEBROOT/.well-known" "$ACME_WEBROOT/.well-known/acme-challenge"
install -d -o root -g root -m 0700 /var/backups/plantahub "$DB_BACKUP_DIR"
ok "estrutura criada"

# ------------------------------------------------------------------ .env

step "Arquivo de ambiente $BACKEND_ENV_FILE"
if [ ! -f "$BACKEND_ENV_FILE" ]; then
  install -o root -g "$APP_USER" -m 0640 "$DEPLOY_DIR/env/backend.env.example" "$BACKEND_ENV_FILE"
  warn "criado a partir do exemplo: PREENCHA os valores TROQUE_* antes de iniciar o backend"
else
  chown root:"$APP_USER" "$BACKEND_ENV_FILE"
  chmod 0640 "$BACKEND_ENV_FILE"
  ok "já existe (conteúdo mantido; dono/permissão corrigidos para root:$APP_USER 640)"
fi
if grep -q $'\r' "$BACKEND_ENV_FILE"; then
  sed -i 's/\r$//' "$BACKEND_ENV_FILE"
  warn "removidos finais de linha CRLF do .env"
fi

# ------------------------------------------------------------------ release script

step "Script de release"
install -o root -g root -m 0755 "$DEPLOY_DIR/scripts/plantahub-release.sh" "$RELEASE_BIN"
ok "$RELEASE_BIN instalado"

# ------------------------------------------------------------------ sudoers

step "sudoers do usuário $DEPLOY_USER"
TMP_SUDOERS="$(mktemp)"
sed "s/^deploy /${DEPLOY_USER} /" "$DEPLOY_DIR/sudoers/plantahub-deploy" > "$TMP_SUDOERS"
visudo -cf "$TMP_SUDOERS" >/dev/null || die "sudoers inválido"
install -o root -g root -m 0440 "$TMP_SUDOERS" /etc/sudoers.d/plantahub-deploy
rm -f "$TMP_SUDOERS"
ok "$DEPLOY_USER pode rodar apenas: sudo $RELEASE_BIN activate|rollback|status"

# ------------------------------------------------------------------ systemd

step "Unit systemd $SERVICE_NAME"
TMP_UNIT="$(mktemp)"
sed -e "s/^User=plantahub$/User=${APP_USER}/" -e "s/^Group=plantahub$/Group=${APP_USER}/" \
  "$DEPLOY_DIR/systemd/plantahub-backend.service" > "$TMP_UNIT"
install -o root -g root -m 0644 "$TMP_UNIT" "/etc/systemd/system/${SERVICE_NAME}.service"
rm -f "$TMP_UNIT"
systemd-analyze verify "/etc/systemd/system/${SERVICE_NAME}.service" 2>&1 | grep -v 'app.jar' || true
systemctl daemon-reload
systemctl enable "$SERVICE_NAME" >/dev/null
ok "unit instalada e habilitada no boot (o start acontece no primeiro deploy)"

# ------------------------------------------------------------------ watchdog

step "Watchdog do backend"
install -o root -g root -m 0755 "$DEPLOY_DIR/scripts/plantahub-watchdog.sh" /usr/local/sbin/plantahub-watchdog
install -o root -g root -m 0644 "$DEPLOY_DIR/systemd/plantahub-watchdog.service" /etc/systemd/system/plantahub-watchdog.service
install -o root -g root -m 0644 "$DEPLOY_DIR/systemd/plantahub-watchdog.timer" /etc/systemd/system/plantahub-watchdog.timer
systemctl daemon-reload
systemctl enable --now plantahub-watchdog.timer >/dev/null
ok "plantahub-watchdog.timer ativo: testa /health a cada minuto e reinicia após ~3 min sem resposta"
info "pausar: touch /etc/plantahub/watchdog.disabled · logs: journalctl -u plantahub-watchdog"

# ------------------------------------------------------------------ logs

# Leitura do journal para o usuário de deploy: permite diagnosticar uma queda (e o
# CI mostrar o motivo de um rollback) sem precisar de root. Só leitura.
if getent group systemd-journal >/dev/null && ! id -nG "$DEPLOY_USER" | grep -qw systemd-journal; then
  usermod -aG systemd-journal "$DEPLOY_USER"
  ok "$DEPLOY_USER pode ler os logs (grupo systemd-journal)"
fi

step "Diretórios prontos"
info "Próximo: preencha $BACKEND_ENV_FILE  →  sudo nano $BACKEND_ENV_FILE"
info "Depois:  sudo bash $DEPLOY_DIR/scripts/04-install-backend.sh /caminho/para/app.jar"
