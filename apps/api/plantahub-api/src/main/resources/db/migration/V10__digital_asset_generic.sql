-- V10__digital_asset_generic.sql

-- 1) colunas novas
ALTER TABLE digital_asset
  ADD COLUMN IF NOT EXISTS media_type VARCHAR(120),
  ADD COLUMN IF NOT EXISTS file_ext VARCHAR(20);

-- 2) remover constraint antiga (se existir)
ALTER TABLE digital_asset
  DROP CONSTRAINT IF EXISTS digital_asset_product_plan_type_id_format_version_key;

-- 3) garantir UNIQUE do storage_key
ALTER TABLE digital_asset
  DROP CONSTRAINT IF EXISTS uq_digital_asset_storage_key;

ALTER TABLE digital_asset
  ADD CONSTRAINT uq_digital_asset_storage_key UNIQUE (storage_key);