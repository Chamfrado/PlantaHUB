ALTER TABLE app_user
    ADD COLUMN active boolean NOT NULL DEFAULT true;

ALTER TABLE app_user
    ADD COLUMN deleted_at timestamptz NULL;

ALTER TABLE app_user
    ADD COLUMN delete_reason varchar(255) NULL;