ALTER TABLE design_requests
    ADD COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'ESCROW_HELD',
    ADD COLUMN payment_method VARCHAR(50) DEFAULT 'VNPAY',
    ADD COLUMN transaction_code VARCHAR(100) NULL,
    ADD COLUMN paid_at DATETIME NULL,
    ADD COLUMN released_at DATETIME NULL,
    ADD COLUMN customer_feedback VARCHAR(1000) NULL,
    ADD COLUMN revision_count INT NOT NULL DEFAULT 0;

