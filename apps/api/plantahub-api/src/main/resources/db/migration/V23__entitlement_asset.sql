-- =========================================================================
-- V23 - O que exatamente cada compra deu direito
--
-- Ate aqui, um entitlement concedia um PAR (produto, colecao) e a lista de
-- arquivos era descoberta na hora do download, listando prefixos no S3. Isso
-- significa que mexer nos arquivos mudava, retroativamente, o que um comprador
-- antigo recebia — e que nao havia como auditar o que foi entregue.
--
-- Agora, no momento do pagamento, a lista exata de arquivos concedidos e
-- congelada aqui. A regra "colecao que acompanha toda oferta" e resolvida uma
-- unica vez, nesse instante, e nao a cada leitura.
-- =========================================================================

CREATE TABLE IF NOT EXISTS entitlement_asset (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  entitlement_id   UUID NOT NULL REFERENCES download_entitlement(id) ON DELETE CASCADE,

  -- Sem ON DELETE CASCADE de proposito: um asset concedido nao pode sumir por
  -- acidente. A remocao pelo admin e logica (digital_asset.deleted_at), e o
  -- caminho de download ignora esse campo justamente para nao quebrar quem ja comprou.
  digital_asset_id UUID NOT NULL REFERENCES digital_asset(id),

  -- PURCHASED: veio da colecao que o cliente escolheu.
  -- BUNDLED:   veio de uma colecao marcada como bundled_with_every_offer.
  source           VARCHAR(20) NOT NULL,

  pinned_at        TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_entitlement_asset UNIQUE (entitlement_id, digital_asset_id),
  CONSTRAINT ck_entitlement_asset_source CHECK (source IN ('PURCHASED', 'BUNDLED'))
);

CREATE INDEX IF NOT EXISTS idx_entitlement_asset_ent ON entitlement_asset(entitlement_id);
CREATE INDEX IF NOT EXISTS idx_entitlement_asset_asset ON entitlement_asset(digital_asset_id);
