#!/usr/bin/env bash
#
# 02 — Cria usuário, banco e privilégios do PlantaHUB no PostgreSQL local. Idempotente.
#
# Resolve os dois erros da VM anterior:
#   - "permission denied for schema public": desde o PostgreSQL 15 o schema public não
#     concede CREATE a todos. Aqui o usuário da aplicação vira DONO do banco e do
#     schema public, que é o que o Flyway precisa para criar tabelas, tipos e índices.
#   - "Peer authentication failed" em `psql -U plantahub`: sem -h, o psql usa socket
#     Unix e autenticação peer (usuário Linux == usuário do banco). A aplicação conecta
#     por TCP em 127.0.0.1 com senha; a validação no fim faz exatamente isso.
#
# Uso:
#   sudo bash deploy/scripts/02-setup-postgresql.sh
#     → pede a senha (ou gera uma com GENERATE_PASSWORD=1)
#   sudo DB_PASSWORD='...' bash deploy/scripts/02-setup-postgresql.sh
#
# A senha aceita [A-Za-z0-9_-], mínimo 20 caracteres (evita problemas de escape em SQL,
# JDBC e no EnvironmentFile do systemd). A senha NUNCA é gravada por este script: copie-a
# para SPRING_DATASOURCE_PASSWORD em /opt/plantahub/backend/.env (fase 4).

. "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_root "$@"
load_conf

valid_identifier "$DB_NAME" || die "DB_NAME inválido: $DB_NAME"
valid_identifier "$DB_USER" || die "DB_USER inválido: $DB_USER"
command -v psql >/dev/null || die "psql não encontrado; rode 01-bootstrap-server.sh"

as_postgres() { (cd / && runuser -u postgres -- "$@"); }

# ------------------------------------------------------------------ senha

ROLE_EXISTS="$(as_postgres psql -tAc "select 1 from pg_roles where rolname = '${DB_USER}'")"
GENERATED=false

if [ -z "${DB_PASSWORD:-}" ]; then
  if [ "${GENERATE_PASSWORD:-0}" = "1" ]; then
    DB_PASSWORD="$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 40)"
    GENERATED=true
  elif [ -t 0 ]; then
    if [ "$ROLE_EXISTS" = "1" ]; then
      info "O usuário ${DB_USER} já existe. ENTER vazio mantém a senha atual."
    fi
    read -r -s -p "Senha para o usuário ${DB_USER} do PostgreSQL: " DB_PASSWORD; echo
    if [ -n "$DB_PASSWORD" ]; then
      read -r -s -p "Confirme a senha: " DB_PASSWORD2; echo
      [ "$DB_PASSWORD" = "$DB_PASSWORD2" ] || die "as senhas não conferem"
    fi
  fi
fi

if [ -z "${DB_PASSWORD:-}" ] && [ "$ROLE_EXISTS" != "1" ]; then
  die "informe DB_PASSWORD (ou GENERATE_PASSWORD=1): o usuário ${DB_USER} ainda não existe"
fi
if [ -n "${DB_PASSWORD:-}" ]; then
  [[ "$DB_PASSWORD" =~ ^[A-Za-z0-9_-]{20,}$ ]] \
    || die "a senha deve ter 20+ caracteres, só [A-Za-z0-9_-]"
fi

# ------------------------------------------------------------------ acesso só local

step "PostgreSQL escutando só em localhost"
PG_VERSION="$(as_postgres psql -tAc 'show server_version_num' | cut -c1-2)"
LISTEN="$(as_postgres psql -tAc 'show listen_addresses')"
info "versão ${PG_VERSION}, listen_addresses='${LISTEN}'"
if [ "$LISTEN" != "localhost" ] && [ "$LISTEN" != "127.0.0.1" ]; then
  as_postgres psql -qc "alter system set listen_addresses = 'localhost'"
  systemctl restart postgresql
  ok "listen_addresses ajustado para 'localhost'"
else
  ok "já restrito a localhost (a porta 5432 também não está liberada no UFW)"
fi

HBA_FILE="$(as_postgres psql -tAc 'show hba_file')"
if ! grep -Eq '^host[[:space:]]+all[[:space:]]+all[[:space:]]+127\.0\.0\.1/32[[:space:]]+scram-sha-256' "$HBA_FILE"; then
  warn "$HBA_FILE não tem a linha padrão 'host all all 127.0.0.1/32 scram-sha-256'. Confira antes de seguir."
else
  ok "pg_hba.conf aceita senha (scram-sha-256) via TCP em 127.0.0.1"
fi

# ------------------------------------------------------------------ usuário e banco

step "Usuário ${DB_USER} e banco ${DB_NAME}"

# A senha vai pelo stdin (não aparece em `ps`) e como literal via format(%L).
{
  printf '\\set ON_ERROR_STOP on\n'
  if [ -n "${DB_PASSWORD:-}" ]; then
    printf "\\\\set app_password '%s'\n" "$DB_PASSWORD"
  fi
  cat <<SQL
SELECT format('CREATE ROLE %I LOGIN', '${DB_USER}')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DB_USER}') \gexec

-- A aplicação não é superusuária nem cria bancos/roles.
ALTER ROLE "${DB_USER}" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
SQL
  if [ -n "${DB_PASSWORD:-}" ]; then
    printf "SELECT format('ALTER ROLE %%I PASSWORD %%L', '%s', :'app_password') \\\\gexec\n" "$DB_USER"
  fi
  cat <<SQL
SELECT format('CREATE DATABASE %I OWNER %I ENCODING %L TEMPLATE template0', '${DB_NAME}', '${DB_USER}', 'UTF8')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}') \gexec

ALTER DATABASE "${DB_NAME}" OWNER TO "${DB_USER}";
REVOKE ALL ON DATABASE "${DB_NAME}" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE "${DB_NAME}" TO "${DB_USER}";
SQL
} | as_postgres psql -q -d postgres
ok "role e banco prontos"

step "Schema public e extensão"
as_postgres psql -q -d "$DB_NAME" <<SQL
\set ON_ERROR_STOP on
-- Dono do schema => CREATE/USAGE garantidos (o que o Flyway precisa).
ALTER SCHEMA public OWNER TO "${DB_USER}";
GRANT ALL ON SCHEMA public TO "${DB_USER}";
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- V1__schema.sql faz CREATE EXTENSION IF NOT EXISTS pgcrypto (gen_random_uuid).
-- Criada aqui pelo superusuário para não depender de a extensão ser "trusted".
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Se o banco veio de um restore feito por outro usuário, devolve a posse dos objetos.
DO \$\$
DECLARE r record;
BEGIN
  FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
    EXECUTE format('ALTER TABLE public.%I OWNER TO %I', r.tablename, '${DB_USER}');
  END LOOP;
  FOR r IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public' LOOP
    EXECUTE format('ALTER SEQUENCE public.%I OWNER TO %I', r.sequence_name, '${DB_USER}');
  END LOOP;
  FOR r IN SELECT t.typname FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
           WHERE n.nspname = 'public' AND t.typtype = 'e' LOOP
    EXECUTE format('ALTER TYPE public.%I OWNER TO %I', r.typname, '${DB_USER}');
  END LOOP;
END
\$\$;
SQL
ok "public pertence a ${DB_USER}; pgcrypto disponível"

# ------------------------------------------------------------------ validação

step "Validação por TCP (o mesmo caminho do Spring)"
if [ -n "${DB_PASSWORD:-}" ]; then
  RESULT="$(PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -p 5432 -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -tA <<'SQL'
select current_user || ' @ ' || current_database();
select 'CREATE em public: ' || has_schema_privilege('public', 'CREATE');
create table plantahub_setup_probe (id int);
drop table plantahub_setup_probe;
select 'criar/remover tabela: ok';
SQL
)"
  printf '%s\n' "$RESULT" | sed 's/^/    /'
  printf '%s' "$RESULT" | grep -q 'CREATE em public: true' || die "o usuário não tem CREATE em public"
  ok "conexão psql -h 127.0.0.1 -U ${DB_USER} -d ${DB_NAME} funcionando"
else
  warn "senha mantida; para validar: psql -h 127.0.0.1 -U ${DB_USER} -d ${DB_NAME}"
fi

if [ "$GENERATED" = true ]; then
  printf '\n%sSenha gerada (mostrada UMA vez — copie agora para o .env):%s\n' "$C_BLD" "$C_RST"
  printf '    SPRING_DATASOURCE_PASSWORD=%s\n\n' "$DB_PASSWORD"
fi

step "PostgreSQL pronto"
info "JDBC: jdbc:postgresql://127.0.0.1:5432/${DB_NAME}  usuário: ${DB_USER}"
info "Próximo: sudo bash $DEPLOY_DIR/scripts/03-setup-directories.sh"
