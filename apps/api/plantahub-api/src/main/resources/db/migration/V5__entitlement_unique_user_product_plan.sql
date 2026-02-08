ALTER TABLE download_entitlement
  ADD CONSTRAINT uk_ent_user_product_plan
  UNIQUE (user_id, product_id, plan_type_id);
