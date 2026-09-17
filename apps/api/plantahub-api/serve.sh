#!/usr/bin/env bash
#
# Sobe a API em modo de desenvolvimento.
#
# As verificações antes do boot não são zelo: cada uma corresponde a uma falha que o
# Spring reporta de um jeito que custa minutos para diagnosticar. Postgres fora do ar vira
# uma pilha de stacktrace de HikariPool; banco inexistente vira um erro de Flyway no meio
# do log. Falhar antes, com uma frase e o comando para resolver, é a diferença toda.
#
# Porta ocupada não é erro: o script procura a próxima livre e anuncia qual usou. Para o
# front não ficar adivinhando, a URL escolhida é gravada num arquivo que o `dev.sh` lê.
#
# Uso:
#   ./serve.sh                 sobe com os padrões (banco plantahub, porta 8080)
#   ./serve.sh --db teste      usa outro banco
#   ./serve.sh --port 8081     tenta esta porta primeiro
#   ./serve.sh --strict-port   falha em vez de procurar outra porta
#   ./serve.sh --create-db     cria o banco se ele não existir (precisa de psql)
#   ./serve.sh --clean         limpa o build antes de subir
#
# Configuração local: crie um `.env` nesta pasta (já ignorado pelo git) com as variáveis
# que quiser sobrescrever. Ele é carregado antes de tudo.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || printf '%s' "$(cd ../../.. && pwd)")"

# Onde o `dev.sh` descobre em que porta a API subiu. Um arquivo, e não uma convenção de
# porta fixa, porque a porta é justamente o que deixou de ser previsível.
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

DB_NAME=''
PORT=''
STRICT_PORT=false
CREATE_DB=false
CLEAN=false

while [ $# -gt 0 ]; do
  case "$1" in
    --db)          DB_NAME="${2:-}"; shift 2 ;;
    --port)        PORT="${2:-}"; shift 2 ;;
    --strict-port) STRICT_PORT=true; shift ;;
    --create-db)   CREATE_DB=true; shift ;;
    --clean)       CLEAN=true; shift ;;
    -h|--help)     sed -n '2,23p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)             die "opção desconhecida: $1 (use --help)" ;;
  esac
done

# ---------------------------------------------------------------- portas

# Conectar é o teste mais barato e mais confiável de "tem alguém escutando": não depende de
# netstat nem de lsof, que reportam de formas diferentes em cada sistema.
port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# Só para nomear o PID na mensagem. Se não der para descobrir, a mensagem sai sem ele.
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

find_free_port() {
  local start="$1" limit="${2:-20}" candidate

  for ((candidate = start; candidate < start + limit; candidate++)); do
    if ! port_in_use "$candidate"; then
      printf '%s' "$candidate"
      return 0
    fi
  done

  return 1
}

# ---------------------------------------------------------------- ambiente

if [ -f .env ]; then
  # `set -a` exporta tudo que for definido no arquivo, então basta listar VAR=valor nele.
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
  ok "Variáveis carregadas de .env"
fi

WANTED_PORT="${PORT:-${SERVER_PORT:-8080}}"

if port_in_use "$WANTED_PORT"; then
  OWNER="$(port_owner "$WANTED_PORT" || true)"
  OWNER_TEXT=""
  [ -n "$OWNER" ] && OWNER_TEXT=" (PID $OWNER)"

  if [ "$STRICT_PORT" = true ]; then
    printf '%s✗%s A porta %s já está em uso%s.\n' "$RED" "$RESET" "$WANTED_PORT" "$OWNER_TEXT" >&2
    hint "Windows: taskkill //PID ${OWNER:-?} //F    ·    Unix: kill ${OWNER:-?}"
    exit 1
  fi

  SERVER_PORT="$(find_free_port "$((WANTED_PORT + 1))")" \
    || die "Nenhuma porta livre entre $((WANTED_PORT + 1)) e $((WANTED_PORT + 20))."

  warn "Porta ${WANTED_PORT} ocupada${OWNER_TEXT} — usando ${SERVER_PORT}."
else
  SERVER_PORT="$WANTED_PORT"
fi

DEFAULT_DB="${DB_NAME:-plantahub}"
SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/${DEFAULT_DB}}"
SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-postgres}"
SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-postgres}"

# `--db` vence uma URL herdada do ambiente: quem passou a flag foi explícito agora.
if [ -n "$DB_NAME" ]; then
  SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL%/*}/${DB_NAME}"
fi

# O front também pode acabar numa porta diferente da preferida, e uma origem fora desta
# lista faz TODA requisição do painel morrer no CORS — que o navegador reporta como erro
# de rede, sem dizer que o problema é a origem. Liberar a faixa inteira de portas de
# desenvolvimento resolve os dois lados do problema de uma vez.
#
# Isto vale só aqui: produção define APP_CORS_ALLOWED_ORIGINS explicitamente.
if [ -z "${APP_CORS_ALLOWED_ORIGINS:-}" ]; then
  DEV_ORIGINS=""
  for p in $(seq 5173 5182) $(seq 4173 4182); do
    DEV_ORIGINS="${DEV_ORIGINS}${DEV_ORIGINS:+,}http://localhost:${p}"
  done
  APP_CORS_ALLOWED_ORIGINS="$DEV_ORIGINS"
fi

# Conta de administrador do ambiente local, criada no boot se ainda nao existir.
#
# A senha vive aqui, e nao no application.yml, porque ela e assunto de quem roda o projeto
# na propria maquina — nao configuracao da aplicacao. E so funciona aqui: o componente que
# cria a conta esta fora do perfil prod, entao estas variaveis nao tem efeito em producao.
# Sobrescreva no .env desta pasta se quiser outras credenciais.
export APP_ADMIN_DEV_ACCOUNT_EMAIL="${APP_ADMIN_DEV_ACCOUNT_EMAIL:-adm@plantahub.com}"
export APP_ADMIN_DEV_ACCOUNT_PASSWORD="${APP_ADMIN_DEV_ACCOUNT_PASSWORD:-123456}"
export APP_ADMIN_DEV_ACCOUNT_NAME="${APP_ADMIN_DEV_ACCOUNT_NAME:-Super Admin}"

export APP_CORS_ALLOWED_ORIGINS
export SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD SERVER_PORT

# ---------------------------------------------------------------- verificações

command -v java >/dev/null 2>&1 || die "Java não encontrado no PATH. O projeto usa Java 21."

JAVA_MAJOR="$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')"
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -lt 21 ] 2>/dev/null; then
  die "Java $JAVA_MAJOR encontrado, mas o projeto exige 21 ou superior."
fi

# Postgres no ar. Sem isto, a falha aparece como um stacktrace de pool de conexão, muito
# depois do boot ter começado.
DB_HOST_PORT="${SPRING_DATASOURCE_URL#*//}"
DB_HOST="${DB_HOST_PORT%%:*}"
DB_REST="${DB_HOST_PORT#*:}"
DB_PORT="${DB_REST%%/*}"
DB="${SPRING_DATASOURCE_URL##*/}"

if ! (exec 3<>"/dev/tcp/${DB_HOST}/${DB_PORT}") 2>/dev/null; then
  printf '%s✗%s PostgreSQL não respondeu em %s:%s.\n' "$RED" "$RESET" "$DB_HOST" "$DB_PORT" >&2
  hint "Suba o banco e rode de novo. A URL usada foi: $SPRING_DATASOURCE_URL"
  exit 1
fi

ok "PostgreSQL respondendo em ${DB_HOST}:${DB_PORT}"

# Criar banco é efeito colateral, então só acontece quando pedido explicitamente.
if command -v psql >/dev/null 2>&1; then
  EXISTS="$(PGPASSWORD="$SPRING_DATASOURCE_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" \
    -U "$SPRING_DATASOURCE_USERNAME" -d postgres -tAc \
    "select 1 from pg_database where datname='${DB}'" 2>/dev/null || true)"

  if [ "$EXISTS" != "1" ]; then
    if [ "$CREATE_DB" = true ]; then
      PGPASSWORD="$SPRING_DATASOURCE_PASSWORD" createdb -h "$DB_HOST" -p "$DB_PORT" \
        -U "$SPRING_DATASOURCE_USERNAME" "$DB"
      ok "Banco '${DB}' criado. O Flyway vai aplicar as migrations no boot."
    else
      printf '%s✗%s O banco "%s" não existe.\n' "$RED" "$RESET" "$DB" >&2
      hint "Crie com: ./serve.sh --create-db"
      exit 1
    fi
  fi
fi

# ---------------------------------------------------------------- publicar a URL

API_URL="http://localhost:${SERVER_PORT}"
printf '%s\n' "$API_URL" > "$API_URL_FILE"

# Some ao encerrar, para o `dev.sh` não seguir um endereço que não existe mais. Ele ainda
# confere se alguém responde ali, mas um arquivo velho gera confusão à toa.
cleanup() {
  [ -f "$API_URL_FILE" ] && [ "$(cat "$API_URL_FILE" 2>/dev/null)" = "$API_URL" ] \
    && rm -f "$API_URL_FILE"
  return 0
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- boot

MVNW='./mvnw'
[ -x "$MVNW" ] || MVNW='sh ./mvnw'

if [ "$CLEAN" = true ]; then
  info "Limpando build anterior…"
  $MVNW -q clean
fi

info ''
info "API      ${API_URL}"
info "Banco    ${SPRING_DATASOURCE_URL}"
info "CORS     localhost:5173-5182 e 4173-4182"
info "Admin    ${APP_ADMIN_DEV_ACCOUNT_EMAIL} / ${APP_ADMIN_DEV_ACCOUNT_PASSWORD}  ${DIM}(so em desenvolvimento)${RESET}"
info ''

# `spring-boot:run` em vez de empacotar: o ciclo de desenvolvimento é recompilar e
# reiniciar, e gerar um jar de 80 MB a cada vez só acrescenta espera.
$MVNW spring-boot:run
