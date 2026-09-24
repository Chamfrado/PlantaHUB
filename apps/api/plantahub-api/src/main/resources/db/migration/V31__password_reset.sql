-- =========================================================================
-- V31 - Recuperacao de senha
--
-- O usuario pede um codigo de 6 digitos por e-mail ou SMS, valida o codigo e
-- recebe um token de uso unico para definir a senha nova.
--
-- Nem o codigo nem o token ficam em texto: so o HMAC-SHA256 deles. Quem ler
-- esta tabela (backup vazado, consulta de suporte) nao consegue usar nada.
-- =========================================================================

-- Tokens JWT emitidos antes desta data sao recusados: trocar a senha derruba
-- todas as sessoes abertas, inclusive a de quem roubou a senha antiga.
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS password_reset_code (
  id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

  -- EMAIL ou SMS: por onde o codigo foi enviado.
  channel                VARCHAR(10) NOT NULL,

  code_hash              VARCHAR(64) NOT NULL,
  expires_at             TIMESTAMP NOT NULL,
  attempts               INT NOT NULL DEFAULT 0,

  -- Preenchido quando o codigo e validado; a partir dai so o token vale.
  verified_at            TIMESTAMP,
  reset_token_hash       VARCHAR(64) UNIQUE,
  reset_token_expires_at TIMESTAMP,

  -- Senha trocada com este registro.
  consumed_at            TIMESTAMP,
  -- Substituido por um pedido mais novo ou bloqueado por tentativas demais.
  invalidated_at         TIMESTAMP,

  request_ip             VARCHAR(45),
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT ck_password_reset_channel CHECK (channel IN ('EMAIL', 'SMS'))
);

CREATE INDEX IF NOT EXISTS idx_password_reset_user ON password_reset_code(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_password_reset_created ON password_reset_code(created_at);
