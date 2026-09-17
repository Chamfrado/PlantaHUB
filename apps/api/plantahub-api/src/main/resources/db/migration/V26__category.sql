-- =========================================================================
-- V26 - Categoria vira entidade
--
-- Ate aqui categoria era uma string solta em product.category, e o rotulo
-- exibido ("Chales") estava escrito no frontend. Duas consequencias:
-- o admin nao conseguia criar uma categoria antes de existir um produto nela
-- (problema do ovo e da galinha), e mudar um rotulo exigia deploy.
-- =========================================================================

CREATE TABLE IF NOT EXISTS category (
  slug            VARCHAR(40) PRIMARY KEY,
  name            VARCHAR(120) NOT NULL,
  description     TEXT,
  sort_order      INT NOT NULL DEFAULT 0,

  -- Permite ao admin montar a home sem mexer em codigo.
  featured_on_home BOOLEAN NOT NULL DEFAULT FALSE,
  home_order       INT NOT NULL DEFAULT 0,

  active          BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT ck_category_slug CHECK (slug ~ '^[a-z][a-z0-9-]{1,39}$')
);

-- Semeia a partir do que ja existe, para nao inventar categoria nenhuma.
INSERT INTO category (slug, name, sort_order, featured_on_home, home_order)
SELECT DISTINCT p.category,
       CASE p.category
           WHEN 'casas'  THEN 'Casas'
           WHEN 'chales' THEN 'Chalés'
           ELSE initcap(p.category)
       END,
       CASE p.category WHEN 'casas' THEN 1 WHEN 'chales' THEN 2 ELSE 99 END,
       TRUE,
       CASE p.category WHEN 'casas' THEN 1 WHEN 'chales' THEN 2 ELSE 99 END
FROM product p
ON CONFLICT (slug) DO NOTHING;

-- A chave estrangeira so entra depois da semeadura, senao derrubaria a migration
-- em qualquer base com uma categoria que nao previmos.
ALTER TABLE product DROP CONSTRAINT IF EXISTS fk_product_category;
ALTER TABLE product
  ADD CONSTRAINT fk_product_category
  FOREIGN KEY (category) REFERENCES category(slug);
