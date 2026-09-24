-- =========================================================================
-- V35 - Categoria "em breve" e ordem dos produtos
--
-- coming_soon: a categoria aparece na pagina de produtos com um aviso de
-- "em construcao", para o visitante saber que vem mais coisa ali, mesmo
-- antes do primeiro produto ser publicado.
--
-- product.sort_order: ate aqui a ordem na vitrine era alfabetica. Agora o
-- admin decide; empates continuam desempatando pelo nome.
-- =========================================================================

ALTER TABLE category ADD COLUMN IF NOT EXISTS coming_soon BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE product ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;

-- Preserva a ordem que o site ja mostrava (alfabetica dentro da categoria).
UPDATE product p
SET sort_order = o.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY category ORDER BY name) AS rn
    FROM product
) o
WHERE o.id = p.id;
