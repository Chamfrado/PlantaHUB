ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_url TEXT,
    ADD COLUMN IF NOT EXISTS payment_invoice_slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_transaction_nsu VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_receipt_url TEXT,
    ADD COLUMN IF NOT EXISTS payment_capture_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_paid_amount_cents BIGINT;