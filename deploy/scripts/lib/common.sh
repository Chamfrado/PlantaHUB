# shellcheck shell=bash
#
# Funções compartilhadas pelos scripts de deploy/scripts/. Não execute diretamente:
#   . "$(dirname "$0")/lib/common.sh"

set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONF_FILE=/etc/plantahub/deploy.conf

if [ -t 1 ]; then
  C_RED=$'\033[31m'; C_YEL=$'\033[33m'; C_GRN=$'\033[32m'; C_BLD=$'\033[1m'; C_RST=$'\033[0m'
else
  C_RED=''; C_YEL=''; C_GRN=''; C_BLD=''; C_RST=''
fi

step() { printf '\n%s==> %s%s\n' "$C_BLD" "$*" "$C_RST"; }
info() { printf '    %s\n' "$*"; }
ok()   { printf '%s  ✓%s %s\n' "$C_GRN" "$C_RST" "$*"; }
warn() { printf '%s  !%s %s\n' "$C_YEL" "$C_RST" "$*" >&2; }
die()  { printf '%s  ✗ %s%s\n' "$C_RED" "$*" "$C_RST" >&2; exit 1; }

trap 'die "falhou na linha $LINENO: $BASH_COMMAND"' ERR

require_root() {
  [ "$(id -u)" -eq 0 ] || die "rode como root: sudo bash $0 $*"
}

require_ubuntu_2404() {
  . /etc/os-release
  if [ "${ID:-}" != "ubuntu" ] || [ "${VERSION_ID:-}" != "24.04" ]; then
    warn "Sistema detectado: ${PRETTY_NAME:-desconhecido}. Estes scripts foram escritos para Ubuntu 24.04."
    [ "${FORCE_OS:-0}" = "1" ] || die "defina FORCE_OS=1 para prosseguir mesmo assim"
  fi
}

# Carrega /etc/plantahub/deploy.conf por cima dos padrões.
load_conf() {
  ROOT_DOMAIN=plantahub.com.br
  WEB_DOMAIN=www.plantahub.com.br
  API_DOMAIN=api.plantahub.com.br
  PUBLIC_IP=''
  LETSENCRYPT_EMAIL=''
  SSH_PORT=22
  APP_USER=plantahub
  DEPLOY_USER=deploy
  DB_NAME=plantahub
  DB_USER=plantahub
  BACKEND_PORT=8080
  HEALTH_PATH=/health
  HEALTH_TIMEOUT_SECONDS=180
  KEEP_RELEASES=5
  KEEP_DB_BACKUPS=10
  DB_BACKUP_BEFORE_DEPLOY=true

  if [ -r "$CONF_FILE" ]; then
    # shellcheck disable=SC1090
    . "$CONF_FILE"
  fi
}

# Caminhos na VM. Mantêm os caminhos da VM anterior (app.jar, .env, frontend/) como
# symlinks/arquivos no mesmo lugar, e acrescentam releases/ para permitir rollback.
BACKEND_BASE=/opt/plantahub/backend
BACKEND_ENV_FILE=$BACKEND_BASE/.env
FRONTEND_BASE=/var/www/plantahub
FRONTEND_ROOT=$FRONTEND_BASE/frontend
INCOMING_BASE=/opt/plantahub/incoming
DB_BACKUP_DIR=/var/backups/plantahub/db
ACME_WEBROOT=/var/www/letsencrypt
RELEASE_BIN=/usr/local/sbin/plantahub-release
SERVICE_NAME=plantahub-backend

# Valida o nome de um usuário/banco antes de interpolar em SQL ou comandos.
valid_identifier() {
  [[ "$1" =~ ^[a-z_][a-z0-9_]{0,62}$ ]]
}
