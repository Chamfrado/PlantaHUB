-- =========================================================================
-- V20 - Fundacao do catalogo administravel
--
-- 1) Ciclo de vida do produto (DRAFT/PUBLISHED/ARCHIVED)
-- 2) plan_type vira "colecao": separa "e um conjunto de arquivos" de "e uma oferta"
-- 3) digital_asset vira a fonte da verdade sobre arquivos
-- 4) entitlement: permite recompra depois de estorno
-- =========================================================================


-- -------------------------------------------------------------------------
-- 1) CICLO DE VIDA DO PRODUTO
--
-- `active` continua existindo e sincronizado por uma release inteira: producao
-- roda com ddl-auto=validate e Product.active e NOT NULL, entao derrubar a coluna
-- junto com a leitura quebraria o boot. A remocao vem numa migration posterior.
-- -------------------------------------------------------------------------
ALTER TABLE product
  ADD COLUMN IF NOT EXISTS status       VARCHAR(20),
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS archived_at  TIMESTAMP;

-- Backfill deliberadamente especifico: um produto inativo que ja foi vendido e
-- ARQUIVADO (precisa continuar resolvendo para pedidos antigos), nao rascunho.
UPDATE product p
SET status = CASE
    WHEN p.active THEN 'PUBLISHED'
    WHEN EXISTS (SELECT 1 FROM order_item oi WHERE oi.product_id = p.id) THEN 'ARCHIVED'
    ELSE 'DRAFT'
END
WHERE p.status IS NULL;

UPDATE product SET published_at = COALESCE(published_at, updated_at) WHERE status = 'PUBLISHED';
UPDATE product SET archived_at  = COALESCE(archived_at,  updated_at) WHERE status = 'ARCHIVED';

ALTER TABLE product ALTER COLUMN status SET NOT NULL;
ALTER TABLE product ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE product DROP CONSTRAINT IF EXISTS ck_product_status;
ALTER TABLE product ADD CONSTRAINT ck_product_status
  CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS idx_product_status ON product(status);


-- -------------------------------------------------------------------------
-- 2) PLAN_TYPE = COLECAO
--
-- purchasable              -> aparece no seletor publico, no carrinho e no checkout
-- bundled_with_every_offer -> os arquivos dela acompanham qualquer oferta do mesmo produto
-- active                   -> o admin pode esconder sem apagar
--
-- Sao tres flags e nao duas de proposito: reaproveitar product_plan_type.is_available
-- para esconder a colecao APOIO funcionaria hoje sem mudar codigo, mas roubaria do
-- admin o unico jeito de marcar uma colecao como "temporariamente fora de venda".
-- -------------------------------------------------------------------------
ALTER TABLE plan_type
  ADD COLUMN IF NOT EXISTS purchasable              BOOLEAN   NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS bundled_with_every_offer BOOLEAN   NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS active                   BOOLEAN   NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS sort_order               INT       NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS updated_at               TIMESTAMP NOT NULL DEFAULT NOW();

-- O code entra em chave S3 legada e no mapeamento pasta->colecao da reconciliacao.
-- Restringir o formato agora evita que o admin crie algo como "Arch / 2" amanha.
ALTER TABLE plan_type DROP CONSTRAINT IF EXISTS ck_plan_type_code;
ALTER TABLE plan_type ADD CONSTRAINT ck_plan_type_code
  CHECK (code ~ '^[A-Z][A-Z0-9_]{1,39}$');

UPDATE plan_type SET sort_order = CASE code
    WHEN 'ARCH' THEN 1
    WHEN 'HYD'  THEN 2
    WHEN 'ELEC' THEN 3
    WHEN 'STR'  THEN 4
    WHEN 'LAND' THEN 5
    ELSE 100
END
WHERE sort_order = 0;


-- -------------------------------------------------------------------------
-- 3) DIGITAL_ASSET
--
-- BLOQUEADOR: uk_asset_ppt_kind_version (criada na V7) permite no maximo UM
-- arquivo por (colecao, kind, version). O bucket tem varios PDFs na mesma pasta,
-- entao tanto a reconciliacao quanto o upload multiplo sao impossiveis ate ela cair.
-- -------------------------------------------------------------------------
ALTER TABLE digital_asset DROP CONSTRAINT IF EXISTS uk_asset_ppt_kind_version;

ALTER TABLE digital_asset
  ADD COLUMN IF NOT EXISTS sort_order            INT         NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS relative_path         VARCHAR(400),
  -- LEGACY = chave que ja existia no bucket, preservada literalmente pela
  -- reconciliacao. V2 = chave gerada pelo upload novo. O runtime NUNCA ramifica
  -- nisto; serve para a UI admin e para uma eventual limpeza futura.
  ADD COLUMN IF NOT EXISTS key_scheme            VARCHAR(20) NOT NULL DEFAULT 'LEGACY',
  ADD COLUMN IF NOT EXISTS reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
  ADD COLUMN IF NOT EXISTS deleted_at            TIMESTAMP,
  ADD COLUMN IF NOT EXISTS created_by            UUID REFERENCES app_user(id),
  ADD COLUMN IF NOT EXISTS etag                  VARCHAR(100);

ALTER TABLE digital_asset DROP CONSTRAINT IF EXISTS ck_digital_asset_key_scheme;
ALTER TABLE digital_asset ADD CONSTRAINT ck_digital_asset_key_scheme
  CHECK (key_scheme IN ('LEGACY', 'V2'));

ALTER TABLE digital_asset ALTER COLUMN kind SET DEFAULT 'FILE';

CREATE INDEX IF NOT EXISTS idx_asset_ppt_active
  ON digital_asset(product_plan_type_id)
  WHERE deleted_at IS NULL;


-- -------------------------------------------------------------------------
-- 4) UNICIDADE DO ENTITLEMENT
--
-- V5 e V9 criaram a MESMA constraint (user, product, plan) com nomes diferentes.
-- Pior: por serem totais, um entitlement revogado continua ocupando o indice,
-- entao um cliente estornado que recompra nao consegue receber um novo direito.
-- O indice parcial resolve as duas coisas.
--
-- uk_entitlement_unique (user, order, product, plan), da V3, esta correta e fica.
-- -------------------------------------------------------------------------
ALTER TABLE download_entitlement DROP CONSTRAINT IF EXISTS uk_ent_user_product_plan;
ALTER TABLE download_entitlement DROP CONSTRAINT IF EXISTS uq_entitlement_user_product_plan;

CREATE UNIQUE INDEX IF NOT EXISTS uq_entitlement_active_user_product_plan
  ON download_entitlement(user_id, product_id, plan_type_id)
  WHERE revoked_at IS NULL;
