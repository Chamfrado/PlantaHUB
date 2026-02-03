-- =========================================================
-- PlantaHUB - Seed (Produtos do products.ts)
-- =========================================================

-- =========================
-- PLAN TYPES (PADRÃO)
-- =========================
INSERT INTO plan_type (code, name, description) VALUES
('ARCH', 'Planta Arquitetônica', 'Layout arquitetônico completo da edificação'),
('HYD',  'Planta Hidráulica',    'Sistema hidráulico e sanitário'),
('ELEC', 'Planta Elétrica',      'Instalações elétricas e pontos'),
('STR',  'Planta Estrutural',    'Estrutura e fundações'),
('LAND', 'Planta Paisagística',  'Projeto paisagístico externo')
ON CONFLICT (code) DO NOTHING;


INSERT INTO product (
  id, slug, category, name, short_desc, hero_image_url,
  area_m2, base_price_cents, delivery, customizable, active
) VALUES
-- CASAS
('casa-confort-80m2',  'confort', 'casas',  'Confort',  'Kitnet Moderna',               'PRODUCT_IMAGES.casas.confort',  80,  0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE),
('casa-prime-150m2',   'prime',   'casas',  'Prime',    'Casa Moderna Familiar',         'PRODUCT_IMAGES.casas.prime',   150, 0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE),
('casa-diamond-300m2', 'diamond', 'casas',  'Diamond',  'Casa Premium de Alto Padrão',   'PRODUCT_IMAGES.casas.diamond', 300, 0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE),

-- CHALÉS
('chale-confort-55m2',   'confort', 'chales', 'Confort', 'Chalé Compacto Aconchegante',          'PRODUCT_IMAGES.chales.confort',  55, 0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE),
('chale-prime-85m2',     'prime',   'chales', 'Prime',   'Chalé Familiar com Varanda',           'PRODUCT_IMAGES.chales.prime',    85, 0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE),
('chale-diamond-120m2',  'diamond', 'chales', 'Diamond', 'Chalé Premium para Locação de Alto Padrão', 'PRODUCT_IMAGES.chales.diamond', 120, 0, 'Download digital imediato na confirmação da compra.', TRUE, TRUE)
ON CONFLICT (id) DO UPDATE SET
  slug = EXCLUDED.slug,
  category = EXCLUDED.category,
  name = EXCLUDED.name,
  short_desc = EXCLUDED.short_desc,
  hero_image_url = EXCLUDED.hero_image_url,
  area_m2 = EXCLUDED.area_m2,
  base_price_cents = EXCLUDED.base_price_cents,
  delivery = EXCLUDED.delivery,
  customizable = EXCLUDED.customizable,
  active = EXCLUDED.active,
  updated_at = NOW();

-- =========================
-- PRODUCT_PLAN_TYPE
-- Associa TODOS os produtos aos 5 tipos padrão.
-- price_cents = NULL (você define depois), is_available = TRUE
-- =========================
WITH all_products AS (
  SELECT id AS product_id
  FROM product
  WHERE id IN (
    'casa-confort-80m2',
    'casa-prime-150m2',
    'casa-diamond-300m2',
    'chale-confort-55m2',
    'chale-prime-85m2',
    'chale-diamond-120m2'
  )
),
all_plan_types AS (
  SELECT id AS plan_type_id, code
  FROM plan_type
  WHERE code IN ('ARCH','HYD','ELEC','STR','LAND')
)
INSERT INTO product_plan_type (
  product_id, plan_type_id, price_cents, is_included_in_bundle, is_available, sort_order
)
SELECT
  p.product_id,
  t.plan_type_id,
  NULL,
  TRUE,
  TRUE,
  CASE t.code
    WHEN 'ARCH' THEN 1
    WHEN 'HYD'  THEN 2
    WHEN 'ELEC' THEN 3
    WHEN 'STR'  THEN 4
    WHEN 'LAND' THEN 5
  END
FROM all_products p
CROSS JOIN all_plan_types t
ON CONFLICT (product_id, plan_type_id) DO NOTHING;