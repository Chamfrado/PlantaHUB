ALTER TABLE orders
ALTER COLUMN status TYPE varchar(32)
    USING status::text;