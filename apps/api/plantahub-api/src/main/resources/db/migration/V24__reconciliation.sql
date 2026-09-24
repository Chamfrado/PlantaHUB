-- =========================================================================
-- V24 - Reconciliacao banco <-> bucket
--
-- O bucket ja contem os arquivos dos produtos existentes. Esta tabela registra
-- cada varredura e o que ela encontrou, para que a migracao dos dados legados
-- seja auditavel e repetivel em vez de um script rodado uma vez e esquecido.
--
-- Regra que o schema ajuda a sustentar: a reconciliacao NUNCA apaga nada. Ela
-- cria linhas em digital_asset apontando para as chaves literais que ja existem
-- no bucket, e registra divergencias como achados para uma pessoa decidir.
-- =========================================================================

CREATE TABLE IF NOT EXISTS reconciliation_run (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  started_by      UUID REFERENCES app_user(id),

  -- Em dry run nada e escrito em digital_asset: so os achados sao registrados.
  dry_run         BOOLEAN NOT NULL,

  -- Opcional: limita a varredura a um produto.
  product_id      VARCHAR(80),

  status          VARCHAR(20) NOT NULL,
  started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  finished_at     TIMESTAMP,

  objects_scanned INT NOT NULL DEFAULT 0,
  assets_created  INT NOT NULL DEFAULT 0,
  assets_updated  INT NOT NULL DEFAULT 0,
  -- Objetos que ja tinham linha correspondente. Contados, nao registrados um a um:
  -- num bucket grande, um achado por arquivo conhecido afogaria o relatorio.
  assets_matched  INT NOT NULL DEFAULT 0,
  findings_count  INT NOT NULL DEFAULT 0,

  error_message   TEXT,

  CONSTRAINT ck_reconciliation_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_run_started ON reconciliation_run(started_at DESC);

CREATE TABLE IF NOT EXISTS reconciliation_finding (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  run_id           UUID NOT NULL REFERENCES reconciliation_run(id) ON DELETE CASCADE,

  type             VARCHAR(40) NOT NULL,
  severity         VARCHAR(10) NOT NULL,

  storage_key      VARCHAR(500),
  product_id       VARCHAR(80),
  plan_type_code   VARCHAR(40),
  digital_asset_id UUID,

  detail           TEXT,

  CONSTRAINT ck_reconciliation_finding_severity CHECK (severity IN ('INFO', 'WARN', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_recon_finding_run ON reconciliation_finding(run_id, type, severity);
