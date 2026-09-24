-- =========================================================================
-- V25 - Conteudo editorial e midia do produto
--
-- Hoje o texto da pagina de cada produto vive num arquivo TypeScript do
-- frontend. Isso e o que obriga um deploy para corrigir uma virgula, e o que
-- impede o Super Admin de criar um produto sem mexer em codigo.
-- =========================================================================


-- -------------------------------------------------------------------------
-- 1) CONTEUDO EDITORIAL  ->  jsonb
--
-- Todo campo aqui e apresentacional, ordenado, opcional e nunca consultado
-- pelo banco: nenhuma tela filtra ou ordena por "o terceiro diferencial".
-- Normalizar custaria cinco tabelas, cinco repositorios, cinco CRUDs e cinco
-- endpoints de reordenacao sem comprar nenhuma capacidade de consulta que
-- alguem use.
--
-- O admin salva isso como um formulario longo e atomico, que e exatamente a
-- forma de transacao do jsonb: um UPDATE. Normalizado, o mesmo salvamento
-- vira um apaga-e-reinsere em cinco tabelas, transacional e com ordem a
-- preservar. A validacao, que e a objecao real, e resolvida por Bean
-- Validation sobre uma arvore de records Java.
-- -------------------------------------------------------------------------
ALTER TABLE product
  ADD COLUMN IF NOT EXISTS content jsonb NOT NULL DEFAULT '{}'::jsonb;


-- -------------------------------------------------------------------------
-- 2) MIDIA  ->  tabela normalizada
--
-- Ao contrario do texto, midia precisa de coisas que sao operacao de linha:
-- o mesmo ciclo de upload dos arquivos, ordenacao com endpoint de reordenar,
-- a invariante "exatamente uma capa" e, no futuro, apagar o objeto no bucket
-- quando a linha sai.
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_media (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id   VARCHAR(80) NOT NULL REFERENCES product(id) ON DELETE CASCADE,

  role         VARCHAR(20) NOT NULL,

  storage_key  VARCHAR(500) NOT NULL,
  public_url   TEXT,
  content_type VARCHAR(120),
  size_bytes   BIGINT,
  width        INT,
  height       INT,
  alt_text     VARCHAR(255),
  sort_order   INT NOT NULL DEFAULT 0,

  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at   TIMESTAMP,

  CONSTRAINT uq_product_media_key UNIQUE (storage_key),
  CONSTRAINT ck_product_media_role CHECK (role IN ('HERO', 'GALLERY'))
);

-- Indice parcial: um produto tem no maximo uma capa viva.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_media_one_hero
  ON product_media(product_id)
  WHERE role = 'HERO' AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_media_product
  ON product_media(product_id, role, sort_order);

-- Backfill da capa a partir da URL que ja estava no produto.
-- product.hero_image_url permanece como cache de leitura: quatro DTOs o leem,
-- e o servico de midia passa a reescreve-lo a cada mudanca de capa.
INSERT INTO product_media (product_id, role, storage_key, public_url, sort_order)
SELECT id, 'HERO', 'products/' || id || '/cover.webp', hero_image_url, 0
FROM product
WHERE hero_image_url IS NOT NULL
ON CONFLICT (storage_key) DO NOTHING;
