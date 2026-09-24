#!/usr/bin/env bash
#
# plantahub-watchdog — reinicia o backend quando ele fica tempo demais sem responder.
#
# Instalado por 03-setup-directories.sh em /usr/local/sbin/plantahub-watchdog e
# executado a cada minuto pelo plantahub-watchdog.timer (como root).
#
# O Restart=always do unit cobre o processo que MORRE. Isto cobre o resto:
#   - processo vivo mas travado (não responde /health);
#   - unit em "failed" depois de estourar o StartLimitBurst (o systemd desiste);
#   - serviço parado por qualquer motivo que não seja uma pausa pedida.
#
# Regras:
#   - reinicia só depois de WATCHDOG_FAILS_BEFORE_RESTART falhas seguidas (1 por minuto);
#   - ignora os primeiros WATCHDOG_STARTUP_GRACE_SECONDS após um start (boot lento em 1 CPU);
#   - no máximo um restart a cada WATCHDOG_COOLDOWN_SECONDS (evita loop);
#   - não faz nada enquanto um plantahub-release (deploy/rollback) estiver rodando;
#   - pausa manual: touch /etc/plantahub/watchdog.disabled  (rm para reativar).

set -uo pipefail
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export PATH LC_ALL=C

BACKEND_PORT=8080
HEALTH_PATH=/health
WATCHDOG_FAILS_BEFORE_RESTART=3
WATCHDOG_STARTUP_GRACE_SECONDS=300
WATCHDOG_COOLDOWN_SECONDS=600
if [ -r /etc/plantahub/deploy.conf ]; then
  # shellcheck disable=SC1091
  . /etc/plantahub/deploy.conf
fi

SERVICE=plantahub-backend
PAUSE_FILE=/etc/plantahub/watchdog.disabled
RELEASE_LOCK=/run/plantahub-release.lock
STATE_DIR=/run/plantahub-watchdog
FAILS_FILE=$STATE_DIR/fails
LAST_RESTART_FILE=$STATE_DIR/last-restart

log() { printf '%s\n' "$*"; }

mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR"

if [ -e "$PAUSE_FILE" ]; then
  exit 0
fi

# Deploy/rollback em andamento: quem manda no serviço agora é o plantahub-release.
if [ -e "$RELEASE_LOCK" ] && ! flock -n "$RELEASE_LOCK" true; then
  exit 0
fi

now="$(date +%s)"
state="$(systemctl show -p ActiveState --value "$SERVICE" 2>/dev/null || echo unknown)"

if [ "$state" = "active" ]; then
  since_us="$(systemctl show -p ActiveEnterTimestampMonotonic --value "$SERVICE" 2>/dev/null || echo 0)"
  uptime_s="$(cut -d' ' -f1 /proc/uptime | cut -d. -f1)"
  running_for=$(( uptime_s - since_us / 1000000 ))
  if [ "$since_us" != "0" ] && [ "$running_for" -lt "$WATCHDOG_STARTUP_GRACE_SECONDS" ]; then
    # Ainda subindo: não conta como falha.
    exit 0
  fi
elif [ "$state" = "activating" ] || [ "$state" = "reloading" ]; then
  exit 0
fi

healthy=false
if [ "$state" = "active" ]; then
  body="$(curl -fsS -m 10 "http://127.0.0.1:${BACKEND_PORT}${HEALTH_PATH}" 2>/dev/null || true)"
  if printf '%s' "$body" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"'; then
    healthy=true
  fi
fi

if [ "$healthy" = true ]; then
  if [ -s "$FAILS_FILE" ] && [ "$(cat "$FAILS_FILE")" != "0" ]; then
    log "backend voltou a responder depois de $(cat "$FAILS_FILE") verificação(ões) com falha"
  fi
  echo 0 > "$FAILS_FILE"
  exit 0
fi

fails=$(( $(cat "$FAILS_FILE" 2>/dev/null || echo 0) + 1 ))
echo "$fails" > "$FAILS_FILE"
log "backend sem resposta ($fails/$WATCHDOG_FAILS_BEFORE_RESTART) — estado do serviço: $state"

if [ "$fails" -lt "$WATCHDOG_FAILS_BEFORE_RESTART" ]; then
  exit 0
fi

last="$(cat "$LAST_RESTART_FILE" 2>/dev/null || echo 0)"
if [ $(( now - last )) -lt "$WATCHDOG_COOLDOWN_SECONDS" ]; then
  log "restart adiado: o último foi há $(( now - last ))s (mínimo ${WATCHDOG_COOLDOWN_SECONDS}s)"
  exit 0
fi

log "REINICIANDO $SERVICE: $fails verificações seguidas sem resposta (estado: $state)"
echo "$now" > "$LAST_RESTART_FILE"
echo 0 > "$FAILS_FILE"
systemctl reset-failed "$SERVICE" 2>/dev/null || true
systemctl restart "$SERVICE" || log "systemctl restart falhou"
exit 0
