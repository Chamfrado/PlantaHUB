#!/usr/bin/env bash
#
# Sobe o site em modo de desenvolvimento.
#
# Duas coisas que o script resolve e que, na mão, custam tempo:
#
# 1. `VITE_API_URL` ausente. O Vite compila sem reclamar e o app monta requisições para
#    `undefined/v1/...` — que o servidor de desenvolvimento responde com o próprio
#    index.html, em HTTP 200. A tela carrega, não mostra erro de rede, e se comporta como
#    se a sessão fosse inválida.
#
# 2. A API numa porta diferente da esperada. O `serve.sh` procura uma porta livre quando a
#    8080 está ocupada e grava a URL escolhida; aqui ela é lida, conferida e usada. Sem
#    isso, o front apontaria para uma porta onde não tem ninguém.
#
# Uso:
#   ./dev.sh                          sobe o site (porta 5173 ou a próxima livre)
#   ./dev.sh --port 5174              tenta esta porta primeiro
#   ./dev.sh --strict-port            falha em vez de procurar outra porta
#   ./dev.sh --api http://host:8080   força a URL da API
#   ./dev.sh --host                   expõe na rede local, para testar no celular
#   ./dev.sh --install                força `npm install` antes de subir

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || printf '%s' "$(cd ../../.. && pwd)")"
API_URL_FILE="${REPO_ROOT}/.plantahub-dev-api-url"

# ---------------------------------------------------------------- saída

if [ -t 1 ]; then
  RED=$'\033[31m'; YELLOW=$'\033[33m'; GREEN=$'\033[32m'; DIM=$'\033[2m'; RESET=$'\033[0m'
else
  RED=''; YELLOW=''; GREEN=''; DIM=''; RESET=''
fi

info() { printf '%s\n' "$*"; }
ok()   { printf '%s✓%s %s\n' "$GREEN" "$RESET" "$*"; }
warn() { printf '%s!%s %s\n' "$YELLOW" "$RESET" "$*"; }
die()  { printf '%s✗%s %s\n' "$RED" "$RESET" "$*" >&2; exit 1; }
hint() { printf '  %s%s%s\n' "$DIM" "$*" "$RESET" >&2; }

# ---------------------------------------------------------------- argumentos

WANTED_PORT='5173'
STRICT_PORT=false
FORCED_API=''
EXPOSE=false
FORCE_INSTALL=false

while [ $# -gt 0 ]; do
  case "$1" in
    --port)        WANTED_PORT="${2:-}"; shift 2 ;;
    --strict-port) STRICT_PORT=true; shift ;;
    --api)         FORCED_API="${2:-}"; shift 2 ;;
    --host)        EXPOSE=true; shift ;;
    --install)     FORCE_INSTALL=true; shift ;;
    -h|--help)     sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)             die "opção desconhecida: $1 (use --help)" ;;
  esac
done

# ---------------------------------------------------------------- pré-requisitos

command -v node >/dev/null 2>&1 || die "Node não encontrado no PATH."
command -v npm  >/dev/null 2>&1 || die "npm não encontrado no PATH."

if [ "$FORCE_INSTALL" = true ] || [ ! -d node_modules ]; then
  info "Instalando dependências…"
  npm install
fi

# ---------------------------------------------------------------- portas

port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

port_owner() {
  local port="$1"

  if command -v netstat >/dev/null 2>&1 && netstat -ano 2>/dev/null | grep -q 'LISTENING'; then
    netstat -ano 2>/dev/null \
      | awk -v p=":${port}$" '$0 ~ /LISTENING/ && $2 ~ p {print $NF}' \
      | head -1
    return
  fi

  if command -v lsof >/dev/null 2>&1; then
    lsof -ti ":$port" -sTCP:LISTEN 2>/dev/null | head -1
  fi
}

if port_in_use "$WANTED_PORT"; then
  OWNER="$(port_owner "$WANTED_PORT" || true)"
  OWNER_TEXT=""
  [ -n "$OWNER" ] && OWNER_TEXT=" (PID $OWNER)"

  if [ "$STRICT_PORT" = true ]; then
    printf '%s✗%s A porta %s já está em uso%s.\n' "$RED" "$RESET" "$WANTED_PORT" "$OWNER_TEXT" >&2
    hint "Windows: taskkill //PID ${OWNER:-?} //F    ·    Unix: kill ${OWNER:-?}"
    exit 1
  fi

  PORT=''
  for ((candidate = WANTED_PORT + 1; candidate < WANTED_PORT + 20; candidate++)); do
    if ! port_in_use "$candidate"; then PORT="$candidate"; break; fi
  done

  [ -n "$PORT" ] || die "Nenhuma porta livre entre $((WANTED_PORT + 1)) e $((WANTED_PORT + 20))."

  warn "Porta ${WANTED_PORT} ocupada${OWNER_TEXT} — usando ${PORT}."
else
  PORT="$WANTED_PORT"
fi

# A faixa que o `serve.sh` libera no CORS por padrão. Fora dela, toda requisição do painel
# é bloqueada pelo navegador — e o erro não menciona a origem.
if [ "$PORT" -lt 5173 ] || [ "$PORT" -gt 5182 ]; then
  warn "A porta ${PORT} está fora da faixa liberada no CORS da API (5173-5182)."
  hint "Suba a API com APP_CORS_ALLOWED_ORIGINS=http://localhost:${PORT}"
fi

# ---------------------------------------------------------------- configuração

# O arquivo é versionado como exemplo justamente para poder ser copiado sem edição: ele já
# aponta para a API local na porta padrão.
if [ ! -f .env.local ]; then
  [ -f .env.example ] || die ".env.local e .env.example não existem — não dá para saber a URL da API."

  cp .env.example .env.local
  ok "Criado .env.local a partir de .env.example"
fi

env_file_url() {
  grep -E '^VITE_API_URL=' .env.local 2>/dev/null \
    | tail -1 | cut -d= -f2- | tr -d '"'"'"' \r'
}

api_answers() {
  command -v curl >/dev/null 2>&1 || return 1
  curl -s -o /dev/null --max-time 2 "${1}/v1/products" 2>/dev/null
}

# Resolve a URL da API na ordem do mais explícito para o mais adivinhado, e diz de onde ela
# veio — saber que o front está apontando para 8081 é metade do trabalho de depurar.
resolve_api() {
  if [ -n "$FORCED_API" ]; then
    API_URL="$FORCED_API"; API_SOURCE="--api"; return
  fi

  if [ -n "${VITE_API_URL:-}" ]; then
    API_URL="$VITE_API_URL"; API_SOURCE="variável do ambiente"; return
  fi

  if [ -f "$API_URL_FILE" ]; then
    local recorded
    recorded="$(cat "$API_URL_FILE" 2>/dev/null || true)"

    # Confere antes de confiar: o arquivo pode ter sobrado de uma API encerrada à força.
    if [ -n "$recorded" ] && api_answers "$recorded"; then
      API_URL="$recorded"; API_SOURCE="serve.sh"; return
    fi
  fi

  local from_file
  from_file="$(env_file_url)"

  if [ -n "$from_file" ] && api_answers "$from_file"; then
    API_URL="$from_file"; API_SOURCE=".env.local"; return
  fi

  # Último recurso: varre a faixa em que o serve.sh procura porta. Cobre o caso de a API
  # ter sido subida à mão, sem o script.
  local p
  for ((p = 8080; p <= 8090; p++)); do
    if api_answers "http://localhost:${p}"; then
      API_URL="http://localhost:${p}"; API_SOURCE="encontrada na porta ${p}"; return
    fi
  done

  API_URL="$from_file"; API_SOURCE="nenhuma API respondeu"
}

resolve_api

if [ -z "$API_URL" ]; then
  printf '%s✗%s Não foi possível determinar a URL da API.\n' "$RED" "$RESET" >&2
  hint 'Sem ela o app pede "undefined/v1/..." e a tela quebra de um jeito que não parece erro de configuração.'
  hint "Preencha VITE_API_URL em .env.local, ou use: ./dev.sh --api http://localhost:8080"
  exit 1
fi

if [ "$API_SOURCE" = "nenhuma API respondeu" ]; then
  # Aviso, e não erro: dá para trabalhar em tela, estilo e teste sem backend. O que não dá
  # é descobrir só depois, ao clicar em algo, que nunca houve API.
  warn "Nenhuma API respondeu. Usando ${API_URL}, de .env.local."
  hint "Suba com: ../../api/plantahub-api/serve.sh"
else
  ok "API em ${API_URL} (${API_SOURCE})"
fi

# O Vite dá precedência a uma variável vinda do ambiente sobre o .env.local, então isto
# aponta o app sem editar o arquivo de ninguém.
export VITE_API_URL="$API_URL"

# ---------------------------------------------------------------- boot

# `--strictPort` porque a porta livre já foi escolhida aqui: deixar o Vite mudar por conta
# própria faria a URL anunciada acima ficar errada.
ARGS=(--port "$PORT" --strictPort)
[ "$EXPOSE" = true ] && ARGS+=(--host)

info ''
info "Site     http://localhost:${PORT}"
info "API      ${API_URL}"
info ''

npm run dev -- "${ARGS[@]}"
