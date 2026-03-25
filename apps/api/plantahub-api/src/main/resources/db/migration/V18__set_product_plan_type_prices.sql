UPDATE product_plan_type ppt
SET price_cents = (
    CASE
        -- CASAS / CONFORT
        WHEN p.category = 'casas'  AND p.slug = 'confort' AND pt.code = 'ARCH' THEN 5000  * 0.30 * 100
        WHEN p.category = 'casas'  AND p.slug = 'confort' AND pt.code = 'STR'  THEN 5000  * 0.40 * 100
        WHEN p.category = 'casas'  AND p.slug = 'confort' AND pt.code = 'HYD'  THEN 5000  * 0.15 * 100
        WHEN p.category = 'casas'  AND p.slug = 'confort' AND pt.code = 'ELEC' THEN 5000  * 0.10 * 100
        WHEN p.category = 'casas'  AND p.slug = 'confort' AND pt.code = 'LAND' THEN 5000  * 0.05 * 100

        -- CASAS / PRIME
        WHEN p.category = 'casas'  AND p.slug = 'prime'   AND pt.code = 'ARCH' THEN 7500  * 0.30 * 100
        WHEN p.category = 'casas'  AND p.slug = 'prime'   AND pt.code = 'STR'  THEN 7500  * 0.40 * 100
        WHEN p.category = 'casas'  AND p.slug = 'prime'   AND pt.code = 'HYD'  THEN 7500  * 0.15 * 100
        WHEN p.category = 'casas'  AND p.slug = 'prime'   AND pt.code = 'ELEC' THEN 7500  * 0.10 * 100
        WHEN p.category = 'casas'  AND p.slug = 'prime'   AND pt.code = 'LAND' THEN 7500  * 0.05 * 100

        -- CASAS / DIAMOND
        WHEN p.category = 'casas'  AND p.slug = 'diamond' AND pt.code = 'ARCH' THEN 10000 * 0.30 * 100
        WHEN p.category = 'casas'  AND p.slug = 'diamond' AND pt.code = 'STR'  THEN 10000 * 0.40 * 100
        WHEN p.category = 'casas'  AND p.slug = 'diamond' AND pt.code = 'HYD'  THEN 10000 * 0.15 * 100
        WHEN p.category = 'casas'  AND p.slug = 'diamond' AND pt.code = 'ELEC' THEN 10000 * 0.10 * 100
        WHEN p.category = 'casas'  AND p.slug = 'diamond' AND pt.code = 'LAND' THEN 10000 * 0.05 * 100

        -- CHALES / CONFORT
        WHEN p.category = 'chales' AND p.slug = 'confort' AND pt.code = 'ARCH' THEN 10000 * 0.30 * 100
        WHEN p.category = 'chales' AND p.slug = 'confort' AND pt.code = 'STR'  THEN 10000 * 0.40 * 100
        WHEN p.category = 'chales' AND p.slug = 'confort' AND pt.code = 'HYD'  THEN 10000 * 0.15 * 100
        WHEN p.category = 'chales' AND p.slug = 'confort' AND pt.code = 'ELEC' THEN 10000 * 0.10 * 100
        WHEN p.category = 'chales' AND p.slug = 'confort' AND pt.code = 'LAND' THEN 10000 * 0.05 * 100

        -- CHALES / PRIME
        WHEN p.category = 'chales' AND p.slug = 'prime'   AND pt.code = 'ARCH' THEN 12500 * 0.30 * 100
        WHEN p.category = 'chales' AND p.slug = 'prime'   AND pt.code = 'STR'  THEN 12500 * 0.40 * 100
        WHEN p.category = 'chales' AND p.slug = 'prime'   AND pt.code = 'HYD'  THEN 12500 * 0.15 * 100
        WHEN p.category = 'chales' AND p.slug = 'prime'   AND pt.code = 'ELEC' THEN 12500 * 0.10 * 100
        WHEN p.category = 'chales' AND p.slug = 'prime'   AND pt.code = 'LAND' THEN 12500 * 0.05 * 100

        -- CHALES / DIAMOND
        WHEN p.category = 'chales' AND p.slug = 'diamond' AND pt.code = 'ARCH' THEN 15000 * 0.30 * 100
        WHEN p.category = 'chales' AND p.slug = 'diamond' AND pt.code = 'STR'  THEN 15000 * 0.40 * 100
        WHEN p.category = 'chales' AND p.slug = 'diamond' AND pt.code = 'HYD'  THEN 15000 * 0.15 * 100
        WHEN p.category = 'chales' AND p.slug = 'diamond' AND pt.code = 'ELEC' THEN 15000 * 0.10 * 100
        WHEN p.category = 'chales' AND p.slug = 'diamond' AND pt.code = 'LAND' THEN 15000 * 0.05 * 100

        ELSE ppt.price_cents
        END
    )::integer
FROM product p, plan_type pt
WHERE p.id = ppt.product_id
  AND pt.id = ppt.plan_type_id;