-- Enhance the existing transactions table for customer/admin transaction history.
-- The project currently uses COMPLETED in the database; APIs expose it as SUCCESS.

ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM(
    'MEMBERSHIP_PURCHASE',
    'MEMBERSHIP_UPGRADE',
    'POST_FEE',
    'PUSH_FEE',
    'REPOST_FEE',
    'WALLET_TOPUP',
    'ROOM_RENT',
    'DEPOSIT',
    'MONTHLY_INVOICE',
    'UTILITY_BILL',
    'SERVICE_FEE',
    'REFUND'
) NOT NULL;

ALTER TABLE transactions
MODIFY COLUMN reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH', 'REPOST', 'INVOICE', 'ROOM', 'CONTRACT', 'WALLET') NOT NULL;

ALTER TABLE transactions
ADD COLUMN payment_method VARCHAR(50) NULL,
ADD COLUMN gateway_response_code VARCHAR(50) NULL,
ADD COLUMN gateway_bank_code VARCHAR(50) NULL,
ADD COLUMN gateway_bank_transaction_id VARCHAR(100) NULL,
ADD COLUMN failure_reason VARCHAR(500) NULL,
ADD COLUMN idempotency_key VARCHAR(120) NULL,
ADD COLUMN invoice_id VARCHAR(36) NULL,
ADD COLUMN invoice_code VARCHAR(50) NULL,
ADD COLUMN landlord_id VARCHAR(36) NULL,
ADD COLUMN room_id BIGINT NULL,
ADD COLUMN room_code VARCHAR(50) NULL,
ADD COLUMN room_name VARCHAR(150) NULL,
ADD COLUMN room_address VARCHAR(500) NULL,
ADD COLUMN customer_name_snapshot VARCHAR(150) NULL,
ADD COLUMN customer_phone_snapshot VARCHAR(30) NULL,
ADD COLUMN landlord_name_snapshot VARCHAR(150) NULL,
ADD COLUMN landlord_phone_snapshot VARCHAR(30) NULL,
ADD COLUMN provider_payload TEXT NULL,
ADD COLUMN completed_at DATETIME(6) NULL,
ADD COLUMN expired_at DATETIME(6) NULL,
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transactions_customer_created ON transactions(user_id, created_at);
CREATE INDEX idx_transactions_landlord_created ON transactions(landlord_id, created_at);
CREATE INDEX idx_transactions_status_created ON transactions(status, created_at);
CREATE INDEX idx_transactions_type_created ON transactions(transaction_type, created_at);
CREATE INDEX idx_transactions_gateway_created ON transactions(payment_provider, created_at);
CREATE INDEX idx_transactions_invoice ON transactions(invoice_id);
CREATE INDEX idx_transactions_invoice_code ON transactions(invoice_code);

CREATE TABLE transaction_audits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    old_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED') NULL,
    new_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED') NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(36) NULL,
    reason VARCHAR(500) NULL,
    provider_event_id VARCHAR(120) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_transaction_audit_transaction (transaction_id, created_at),
    CONSTRAINT fk_transaction_audit_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
