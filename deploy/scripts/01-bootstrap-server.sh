#!/usr/bin/env bash
#
# 01 — Prepara uma VM Ubuntu 24.04 vazia para o PlantaHUB. Idempotente.
#
# Instala só o necessário para EXECUTAR (sem Node, sem Maven: o build é no GitHub
# Actions), cria os usuários, configura UFW, swap e limite do journal.
#
# Uso:
#   sudo PUBLIC_IP=203.0.113.10 LETSENCRYPT_EMAIL=voce@exemplo.com \
#        DEPLOY_PUBKEY="ssh-ed25519 AAAA... github-actions-plantahub" \
#        bash deploy/scripts/01-bootstrap-server.sh
#
# Todas as variáveis são opcionais aqui; PUBLIC_IP e LETSENCRYPT_EMAIL podem ser
# preenchidos depois em /etc/plantahub/deploy.conf. DEPLOY_PUBKEY é a chave PÚBLICA
# (nunca a privada) que o GitHub Actions usará para entrar como "deploy".
#
# Variáveis de controle: SKIP_UFW=1, SKIP_SWAP=1, SWAP_SIZE=2G, FORCE_OS=1.

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
require_ubuntu_2404

# ------------------------------------------------------------------ configuração

step "Configuração em $CONF_FILE"
mkdir -p /etc/plantahub
chmod 755 /etc/plantahub

if [ ! -f "$CONF_FILE" ]; then
  install -o root -g root -m 0644 "$DEPLOY_DIR/env/deploy.conf.example" "$CONF_FILE"
  ok "criado a partir de deploy/env/deploy.conf.example"
else
  ok "já existe (mantido)"
fi

set_conf() {
  local key="$1" value="$2"
  [ -n "$value" ] || return 0
  if grep -q "^${key}=" "$CONF_FILE"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$CONF_FILE"
  else
    printf '%s=%s\n' "$key" "$value" >> "$CONF_FILE"
  fi
  ok "$key=$value"
}

if [ -n "${PUBLIC_IP:-}" ]; then
  [[ "$PUBLIC_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || die "PUBLIC_IP inválido: $PUBLIC_IP"
fi
if [ -n "${LETSENCRYPT_EMAIL:-}" ]; then
  [[ "$LETSENCRYPT_EMAIL" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]] || die "LETSENCRYPT_EMAIL inválido"
fi
set_conf PUBLIC_IP "${PUBLIC_IP:-}"
set_conf LETSENCRYPT_EMAIL "${LETSENCRYPT_EMAIL:-}"

load_conf
valid_identifier "$APP_USER" || die "APP_USER inválido"
valid_identifier "$DEPLOY_USER" || die "DEPLOY_USER inválido"
[[ "$SSH_PORT" =~ ^[0-9]{1,5}$ ]] || die "SSH_PORT inválido"

# ------------------------------------------------------------------ pacotes

step "Pacotes do sistema"
export DEBIAN_FRONTEND=noninteractive
# Numa VM recém-criada o unattended-upgrades costuma estar rodando e segura o lock do
# dpkg por vários minutos; em vez de falhar, espera até 15 min por ele.
APT_WAIT=(-o DPkg::Lock::Timeout=900)
# (Não use pgrep "unattended-upgrade": o unattended-upgrade-shutdown --wait-for-signal
# fica rodando para sempre. O que importa é quem segura o lock do dpkg.)
if fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1; then
  info "atualizações automáticas do Ubuntu em andamento; o apt vai aguardar elas terminarem"
fi
apt-get "${APT_WAIT[@]}" update -q
# openjdk-21-jdk-headless = JDK completo (inclui javac). Um JRE sozinho já causou
# "No compiler is provided in this environment" na VM anterior.
apt-get "${APT_WAIT[@]}" install -y -q --no-install-recommends \
  openjdk-21-jdk-headless \
  postgresql postgresql-client \
  nginx \
  certbot python3-certbot-nginx \
  rsync curl git unzip ca-certificates openssl \
  ufw dnsutils util-linux
ok "pacotes instalados"

# ------------------------------------------------------------------ Java

step "Java 21 (JDK)"
JAVA_REAL="$(readlink -f /usr/bin/java)"
if [[ "$JAVA_REAL" != *java-21* ]]; then
  CANDIDATE="$(update-alternatives --list java | grep java-21 | head -n 1 || true)"
  [ -n "$CANDIDATE" ] || die "Java 21 não encontrado nas alternatives"
  update-alternatives --set java "$CANDIDATE"
  JAVAC_CANDIDATE="$(update-alternatives --list javac | grep java-21 | head -n 1 || true)"
  [ -n "$JAVAC_CANDIDATE" ] && update-alternatives --set javac "$JAVAC_CANDIDATE"
fi
java -version 2>&1 | head -n 1
javac -version 2>&1 | head -n 1
java -version 2>&1 | grep -q 'version "21' || die "java -version não é 21"
javac -version 2>&1 | grep -q 'javac 21' || die "javac ausente ou não é 21 (instale o JDK, não só o JRE)"
ok "/usr/bin/java → $(readlink -f /usr/bin/java)"

# ------------------------------------------------------------------ usuários

step "Usuários"
if ! id "$APP_USER" >/dev/null 2>&1; then
  useradd --system --user-group --home-dir /nonexistent --no-create-home \
    --shell /usr/sbin/nologin "$APP_USER"
  ok "usuário de sistema $APP_USER criado (sem login, roda o backend)"
else
  ok "$APP_USER já existe"
fi

if ! id "$DEPLOY_USER" >/dev/null 2>&1; then
  useradd --create-home --user-group --shell /bin/bash "$DEPLOY_USER"
  passwd -l "$DEPLOY_USER" >/dev/null
  ok "usuário $DEPLOY_USER criado (só chave SSH, senha bloqueada)"
else
  ok "$DEPLOY_USER já existe"
fi

DEPLOY_HOME="$(getent passwd "$DEPLOY_USER" | cut -d: -f6)"
install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 0700 "$DEPLOY_HOME/.ssh"
touch "$DEPLOY_HOME/.ssh/authorized_keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_HOME/.ssh/authorized_keys"
chmod 600 "$DEPLOY_HOME/.ssh/authorized_keys"

if [ -n "${DEPLOY_PUBKEY:-}" ]; then
  if [[ "$DEPLOY_PUBKEY" == *"PRIVATE KEY"* ]]; then
    die "DEPLOY_PUBKEY contém uma chave PRIVADA. Passe o conteúdo do arquivo .pub."
  fi
  [[ "$DEPLOY_PUBKEY" =~ ^(ssh-ed25519|ssh-rsa|ecdsa-sha2-nistp[0-9]+)\ [A-Za-z0-9+/=]+ ]] \
    || die "DEPLOY_PUBKEY não parece uma chave pública OpenSSH"
  if grep -qF "$DEPLOY_PUBKEY" "$DEPLOY_HOME/.ssh/authorized_keys"; then
    ok "chave pública já autorizada para $DEPLOY_USER"
  else
    printf '%s\n' "$DEPLOY_PUBKEY" >> "$DEPLOY_HOME/.ssh/authorized_keys"
    ok "chave pública adicionada a $DEPLOY_HOME/.ssh/authorized_keys"
  fi
else
  warn "DEPLOY_PUBKEY não informado: adicione a chave pública do GitHub Actions depois em $DEPLOY_HOME/.ssh/authorized_keys"
fi

# ------------------------------------------------------------------ firewall

step "UFW (firewall do Ubuntu)"
if [ "${SKIP_UFW:-0}" = "1" ]; then
  warn "SKIP_UFW=1: UFW não alterado"
else
  SSHD_PORTS="$(ss -H -tlnp 2>/dev/null | awk '/sshd/ {n=split($4,a,":"); print a[n]}' | sort -u | tr '\n' ' ')"
  if [ -n "$SSHD_PORTS" ] && [[ " $SSHD_PORTS " != *" $SSH_PORT "* ]]; then
    die "o sshd escuta em [$SSHD_PORTS] mas SSH_PORT=$SSH_PORT. Ajuste $CONF_FILE para não se trancar para fora."
  fi
  ufw default deny incoming >/dev/null
  ufw default allow outgoing >/dev/null
  ufw allow "${SSH_PORT}/tcp" comment 'SSH' >/dev/null
  ufw allow 80/tcp comment 'HTTP (ACME + redirect)' >/dev/null
  ufw allow 443/tcp comment 'HTTPS' >/dev/null
  # 5432 (PostgreSQL) e 8080 (Spring) ficam fechadas: não há regra para elas.
  ufw --force enable >/dev/null
  ufw status verbose
  ok "UFW: só ${SSH_PORT}, 80 e 443 abertas"
fi
warn "O FIREWALL DO PROVEDOR (painel da nuvem) é independente do UFW: libere 22/80/443 lá também."

# ------------------------------------------------------------------ swap

step "Swap"
MEM_MB="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
info "memória: ${MEM_MB} MB"
if [ "${SKIP_SWAP:-0}" = "1" ]; then
  warn "SKIP_SWAP=1: swap não alterado"
elif swapon --show --noheadings | grep -q .; then
  ok "já existe swap: $(swapon --show --noheadings | awk '{print $1" "$3}' | tr '\n' ' ')"
elif [ "$MEM_MB" -lt 4096 ]; then
  SWAP_SIZE="${SWAP_SIZE:-2G}"
  fallocate -l "$SWAP_SIZE" /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  printf 'vm.swappiness=10\n' > /etc/sysctl.d/90-plantahub-swap.conf
  sysctl -q -p /etc/sysctl.d/90-plantahub-swap.conf
  ok "swapfile de $SWAP_SIZE criado (VM com menos de 4 GB: JVM + PostgreSQL)"
else
  ok "memória suficiente; swap não criado"
fi

# ------------------------------------------------------------------ journald

step "Limite do journal"
mkdir -p /etc/systemd/journald.conf.d
cat > /etc/systemd/journald.conf.d/90-plantahub.conf <<'EOF'
[Journal]
SystemMaxUse=500M
EOF
systemctl restart systemd-journald
ok "journal limitado a 500 MB"

# ------------------------------------------------------------------ serviços

step "Serviços base"
systemctl enable --now postgresql >/dev/null
systemctl enable --now nginx >/dev/null
ok "postgresql: $(systemctl is-active postgresql) · nginx: $(systemctl is-active nginx)"

step "Bootstrap concluído"
info "Próximo: sudo bash $DEPLOY_DIR/scripts/02-setup-postgresql.sh"
