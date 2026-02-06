-- =========================================================
-- PlantaHUB - Schema (PostgreSQL) - V1
-- =========================================================

-- UUID helper
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- ENUMS
-- =========================
DO $$ BEGIN
  CREATE TYPE file_format AS ENUM ('BIM', 'DWG', 'PDF');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'CANCELED', 'REFUNDED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(200) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- PRODUCTS (CATÁLOGO)
-- =========================
CREATE TABLE IF NOT EXISTS product (
  id VARCHAR(80) PRIMARY KEY,
  slug VARCHAR(120) NOT NULL,
  category VARCHAR(40) NOT NULL,
  name VARCHAR(160) NOT NULL,
  short_desc VARCHAR(255) NOT NULL,
  hero_image_url TEXT,
  area_m2 INT NOT NULL,
  base_price_cents INT NOT NULL DEFAULT 0,
  delivery TEXT,
  customizable BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (category, slug)
);

CREATE INDEX IF NOT EXISTS idx_product_category ON product(category);
CREATE INDEX IF NOT EXISTS idx_product_slug ON product(slug);

-- =========================
-- PLAN TYPES
-- =========================
CREATE TABLE IF NOT EXISTS plan_type (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(40) UNIQUE NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT
);

-- =========================
-- PRODUCT x PLAN TYPE
-- =========================
CREATE TABLE IF NOT EXISTS product_plan_type (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id VARCHAR(80) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  plan_type_id UUID NOT NULL REFERENCES plan_type(id),
  price_cents INT NOT NULL DEFAULT 0,
  is_included_in_bundle BOOLEAN NOT NULL DEFAULT TRUE,
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  UNIQUE (product_id, plan_type_id)
);

CREATE INDEX IF NOT EXISTS idx_ppt_product ON product_plan_type(product_id);
CREATE INDEX IF NOT EXISTS idx_ppt_plan_type ON product_plan_type(plan_type_id);

-- =========================
-- DIGITAL ASSETS
-- =========================
CREATE TABLE IF NOT EXISTS digital_asset (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_plan_type_id UUID NOT NULL REFERENCES product_plan_type(id) ON DELETE CASCADE,
  format file_format NOT NULL,
  version INT NOT NULL DEFAULT 1,
  filename VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  size_bytes BIGINT,
  checksum_sha256 VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (product_plan_type_id, format, version)
);

CREATE INDEX IF NOT EXISTS idx_asset_ppt ON digital_asset(product_plan_type_id);

-- =========================
-- ORDERS (Checkout)
-- =========================
CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id),
  status order_status NOT NULL DEFAULT 'PENDING',
  total_cents INT NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  paid_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

CREATE TABLE IF NOT EXISTS order_item (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id VARCHAR(80) NOT NULL REFERENCES product(id),
  quantity INT NOT NULL DEFAULT 1,
  unit_price_cents INT NOT NULL DEFAULT 0,
  total_cents INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);

CREATE TABLE IF NOT EXISTS order_item_selection (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_item_id UUID NOT NULL REFERENCES order_item(id) ON DELETE CASCADE,
  plan_type_id UUID NOT NULL REFERENCES plan_type(id),
  price_cents INT NOT NULL DEFAULT 0,
  chosen_format file_format,
  UNIQUE (order_item_id, plan_type_id)
);

CREATE INDEX IF NOT EXISTS idx_ois_order_item_id ON order_item_selection(order_item_id);

-- =========================
-- DOWNLOAD ENTITLEMENTS
-- =========================
CREATE TABLE IF NOT EXISTS download_entitlement (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id),
  order_id UUID NOT NULL REFERENCES orders(id),
  product_id VARCHAR(80) NOT NULL REFERENCES product(id),
  plan_type_id UUID NOT NULL REFERENCES plan_type(id),
  granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
  revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_entitlement_user ON download_entitlement(user_id);
