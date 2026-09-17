-- =========================================================================
-- V29 - Uploads em andamento
--
-- A aplicacao nunca transporta os bytes: ela assina uma URL e o navegador
-- envia direto ao bucket. Esta tabela e o que torna isso seguro.
--
-- A chave de destino e escolhida pelo SERVIDOR e gravada aqui ANTES de os
-- bytes existirem. Sem isso, a confirmacao do upload seria apenas "o cliente
-- disse que gravou em tal lugar" — e um cliente malicioso poderia apontar o
-- registro para qualquer objeto do bucket.
-- =========================================================================

CREATE TABLE IF NOT EXISTS pending_upload (
  id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by             UUID NOT NULL REFERENCES app_user(id),

  -- ASSET (arquivo entregue ao cliente) ou MEDIA (imagem de vitrine).
  target_kind            VARCHAR(20) NOT NULL,

  product_id             VARCHAR(80) REFERENCES product(id),
  product_plan_type_id   UUID REFERENCES product_plan_type(id),

  -- Segmento aleatorio que torna a chave unica. NAO e o id da linha: o id e gerado
  -- pelo banco na confirmacao, e pre-atribui-lo faria o JPA tratar a insercao como
  -- atualizacao de uma linha inexistente.
  key_segment            UUID NOT NULL,

  storage_key            VARCHAR(500) NOT NULL UNIQUE,
  original_filename      VARCHAR(255) NOT NULL,
  relative_path          VARCHAR(400),

  -- "Declarado" porque vem do cliente. Na confirmacao, o tamanho real e lido
  -- do proprio bucket: o valor daqui so decide entre PUT unico e multipart.
  declared_content_type  VARCHAR(120),
  declared_size_bytes    BIGINT,

  s3_multipart_upload_id VARCHAR(255),

  status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  expires_at             TIMESTAMP NOT NULL,
  confirmed_at           TIMESTAMP,

  CONSTRAINT ck_pending_upload_kind CHECK (target_kind IN ('ASSET', 'MEDIA')),
  CONSTRAINT ck_pending_upload_status CHECK (status IN ('PENDING', 'CONFIRMED', 'ABORTED', 'EXPIRED'))
);

-- Usado pelo varredor de uploads abandonados.
CREATE INDEX IF NOT EXISTS idx_pending_upload_sweep ON pending_upload(status, expires_at);
