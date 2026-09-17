-- Segredos operacionais que o painel pode definir, hoje só as credenciais da S3.
--
-- O valor é gravado CIFRADO (AES-GCM); esta tabela nunca vê texto claro, e a API nunca
-- devolve o valor de volta — só uma dica mascarada e a data da última troca.
--
-- Guardar chave de longa duração aqui é um degrau abaixo de uma instance role, que não
-- tem segredo nenhum para vazar. A coluna existe porque nem todo ambiente tem role, não
-- porque seja o caminho preferido.
CREATE TABLE app_secret (
    name            VARCHAR(80)  PRIMARY KEY,
    value_encrypted TEXT         NOT NULL,
    -- Últimos caracteres da parte pública (a access key), para a tela conseguir dizer
    -- QUAL credencial está em uso sem nunca revelar a secret.
    hint            VARCHAR(40),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255)
);

COMMENT ON TABLE app_secret IS
    'Segredos definidos pelo painel, cifrados em repouso. Nunca devolvidos pela API.';
