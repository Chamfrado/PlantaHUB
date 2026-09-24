-- =========================================================================
-- V27 - Pedidos param de depender do catalogo vivo
--
-- order_item guardava apenas uma FK para o produto e os precos. Renomear um
-- produto reescrevia, retroativamente, o historico de todos os pedidos que o
-- continham — e a tela de pedidos exibia ids crus porque nao havia nome para
-- mostrar sem consultar o catalogo atual.
--
-- Com o Super Admin, editar nome e slug deixa de ser excecao e vira rotina.
-- =========================================================================

ALTER TABLE order_item
  ADD COLUMN IF NOT EXISTS product_name_snapshot     VARCHAR(160),
  ADD COLUMN IF NOT EXISTS product_slug_snapshot     VARCHAR(120),
  ADD COLUMN IF NOT EXISTS product_category_snapshot VARCHAR(40),
  ADD COLUMN IF NOT EXISTS product_image_snapshot    TEXT;

ALTER TABLE order_item_selection
  ADD COLUMN IF NOT EXISTS plan_type_code_snapshot VARCHAR(40),
  ADD COLUMN IF NOT EXISTS plan_type_name_snapshot VARCHAR(120);

-- Backfill com o estado atual do catalogo. Nao e o valor historico real — essa
-- informacao nao existe mais —, mas e a melhor aproximacao disponivel, e a
-- partir de agora todo pedido novo grava o valor do momento da compra.
UPDATE order_item oi
SET product_name_snapshot     = p.name,
    product_slug_snapshot     = p.slug,
    product_category_snapshot = p.category,
    product_image_snapshot    = p.hero_image_url
FROM product p
WHERE p.id = oi.product_id
  AND oi.product_name_snapshot IS NULL;

UPDATE order_item_selection ois
SET plan_type_code_snapshot = pt.code,
    plan_type_name_snapshot = pt.name
FROM plan_type pt
WHERE pt.id = ois.plan_type_id
  AND ois.plan_type_code_snapshot IS NULL;
