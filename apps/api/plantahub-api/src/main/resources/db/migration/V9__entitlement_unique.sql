ALTER TABLE download_entitlement
  ADD CONSTRAINT uq_entitlement_user_product_plan
  UNIQUE (user_id, product_id, plan_type_id);