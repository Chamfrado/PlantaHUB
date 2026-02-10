-- 1) adiciona coluna nova flexível
ALTER TABLE digital_asset
  ADD COLUMN IF NOT EXISTS kind VARCHAR(50);

-- 2) copia o enum antigo para o novo campo
UPDATE digital_asset
SET kind = format::text
WHERE kind IS NULL;

-- 3) torna obrigatório
ALTER TABLE digital_asset
  ALTER COLUMN kind SET NOT NULL;

-- 4) remove constraint unique antiga e recria com kind
ALTER TABLE digital_asset
  DROP CONSTRAINT IF EXISTS digital_asset_product_plan_type_id_format_version_key;

ALTER TABLE digital_asset
  ADD CONSTRAINT uk_asset_ppt_kind_version
  UNIQUE (product_plan_type_id, kind, version);

-- 5) remove coluna enum format (opcional, mas recomendado)
ALTER TABLE digital_asset
  DROP COLUMN IF EXISTS format;

-- 6) opcional: remover o TYPE file_format (se não usado em nenhum outro lugar)
DROP TYPE IF EXISTS file_format;
