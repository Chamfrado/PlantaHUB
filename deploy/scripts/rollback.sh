#!/usr/bin/env bash
#
# Rollback manual para a release anterior (troca current ↔ previous).
#
# Uso:
#   sudo bash deploy/scripts/rollback.sh backend
#   sudo bash deploy/scripts/rollback.sh frontend
#   sudo bash deploy/scripts/rollback.sh all
#   sudo bash deploy/scripts/rollback.sh status
#
# Rodar duas vezes seguidas volta para onde estava (current e previous se alternam).
#
# O BANCO NÃO É REVERTIDO. As migrations Flyway são só "para frente"; um jar antigo
# convive com colunas/tabelas novas (Flyway ignora versões futuras e o Hibernate
# `validate` só exige o que o jar antigo conhece). Se a release nova tiver removido ou
# renomeado algo, restaure o dump pré-deploy — ver deploy/README.md, fase 14.

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"

[ -x "$RELEASE_BIN" ] || die "$RELEASE_BIN não instalado; rode 03-setup-directories.sh"

case "${1:-}" in
  backend|frontend|all) "$RELEASE_BIN" rollback "$1"; "$RELEASE_BIN" status ;;
  status)               "$RELEASE_BIN" status ;;
  *)                    die "uso: $0 backend|frontend|all|status" ;;
esac
