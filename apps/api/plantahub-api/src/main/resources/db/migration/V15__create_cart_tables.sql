CREATE TABLE cart (
                      id uuid PRIMARY KEY,
                      user_id uuid NOT NULL,
                      status varchar(32) NOT NULL,
                      created_at timestamptz NOT NULL,
                      updated_at timestamptz NOT NULL,
                      CONSTRAINT fk_cart_user
                          FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE cart_item (
                           id uuid PRIMARY KEY,
                           cart_id uuid NOT NULL,
                           product_id varchar(80) NOT NULL,
                           created_at timestamptz NOT NULL,
                           CONSTRAINT fk_cart_item_cart
                               FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
                           CONSTRAINT fk_cart_item_product
                               FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE cart_item_selection (
                                     id uuid PRIMARY KEY,
                                     cart_item_id uuid NOT NULL,
                                     plan_type_id uuid NOT NULL,
                                     CONSTRAINT fk_cart_item_selection_item
                                         FOREIGN KEY (cart_item_id) REFERENCES cart_item(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_cart_item_selection_plan_type
                                         FOREIGN KEY (plan_type_id) REFERENCES plan_type(id)
);

CREATE UNIQUE INDEX uq_cart_active_user
    ON cart(user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_cart_item_product_per_cart
    ON cart_item(cart_id, product_id);

CREATE UNIQUE INDEX uq_cart_item_selection_unique
    ON cart_item_selection(cart_item_id, plan_type_id);

CREATE INDEX idx_cart_user_status
    ON cart(user_id, status);

CREATE INDEX idx_cart_item_cart
    ON cart_item(cart_id);

CREATE INDEX idx_cart_item_selection_item
    ON cart_item_selection(cart_item_id);