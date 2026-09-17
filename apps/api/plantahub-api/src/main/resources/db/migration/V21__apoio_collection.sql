-- =========================================================================
-- V21 - APOIO deixa de ser string no codigo e vira dado
--
-- Ate aqui, "quem compra qualquer plano leva tambem a pasta APOIO" era um
-- literal "products/{id}/APOIO/" repetido em tres servicos. Depois desta
-- migration a regra e uma linha de tabela com uma flag, e o codigo que a
-- aplicava some na fase seguinte.
-- =========================================================================

INSERT INTO plan_type (code, name, description, purchasable, bundled_with_every_offer, active, sort_order)
VALUES (
    'APOIO',
    'Documentos de Apoio',
    'Arquivos complementares entregues junto de qualquer colecao comprada deste produto.',
    FALSE,  -- nunca aparece no seletor publico nem pode ser comprada avulsa
    TRUE,   -- acompanha toda oferta do mesmo produto
    TRUE,
    900
)
ON CONFLICT (code) DO UPDATE SET
    name                     = EXCLUDED.name,
    description              = EXCLUDED.description,
    purchasable              = EXCLUDED.purchasable,
    bundled_with_every_offer = EXCLUDED.bundled_with_every_offer,
    sort_order               = EXCLUDED.sort_order,
    updated_at               = NOW();

-- digital_asset so consegue se ancorar num product_plan_type, entao cada produto
-- precisa da sua linha (produto, APOIO) para ter onde pendurar esses arquivos.
-- is_available = FALSE porque isto nunca e uma oferta; e um anexo.
INSERT INTO product_plan_type (product_id, plan_type_id, price_cents, is_included_in_bundle, is_available, sort_order)
SELECT p.id, pt.id, 0, FALSE, FALSE, 900
FROM product p
CROSS JOIN plan_type pt
WHERE pt.code = 'APOIO'
ON CONFLICT (product_id, plan_type_id) DO NOTHING;
