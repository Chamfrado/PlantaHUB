-- garante que existe o vínculo product_plan_type para (casa-confort-80m2 + ARCH)
WITH ppt AS (
  SELECT ppt.id
  FROM product_plan_type ppt
  JOIN plan_type pt ON pt.id = ppt.plan_type_id
  WHERE ppt.product_id = 'casa-confort-80m2'
    AND pt.code = 'ARCH'
  LIMIT 1
)
INSERT INTO digital_asset (
  product_plan_type_id, format, version, filename, storage_key, created_at
)
SELECT
  ppt.id,
  'PDF'::file_format,
  1,
  'planta-arquitetonica.pdf',
  'products/casa-confort-80m2/ARCH/v1/planta-arquitetonica.pdf',
  NOW()
FROM ppt
ON CONFLICT (product_plan_type_id, format, version) DO UPDATE SET
  filename = EXCLUDED.filename,
  storage_key = EXCLUDED.storage_key;
