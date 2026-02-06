-- Impede entitlement duplicado para o mesmo usuário/pedido/produto/tipo
ALTER TABLE download_entitlement
  ADD CONSTRAINT uk_entitlement_unique
  UNIQUE (user_id, order_id, product_id, plan_type_id);
