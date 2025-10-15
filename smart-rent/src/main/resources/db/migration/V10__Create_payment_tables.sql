-- Create payment tables for multi-provider payment gateway integration

-- 1. Generic payments table (core payment data)
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    listing_id BIGINT,
    transaction_ref VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(20) NOT NULL,
    provider_transaction_id VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    transaction_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_info VARCHAR(500),
    payment_method VARCHAR(50),
    payment_date DATETIME,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    return_url VARCHAR(1000),
    cancel_url VARCHAR(1000),
    notes TEXT,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_payment_user_id (user_id),
    INDEX idx_payment_listing_id (listing_id),
    INDEX idx_payment_transaction_ref (transaction_ref),
    INDEX idx_payment_provider (provider),
    INDEX idx_payment_provider_transaction_id (provider_transaction_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_created_at (created_at),

    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_currency CHECK (currency IN ('VND', 'USD', 'EUR')),
    CONSTRAINT chk_payment_provider CHECK (provider IN ('VNPAY', 'PAYPAL', 'MOMO')),
    CONSTRAINT chk_payment_transaction_type CHECK (transaction_type IN ('PAYMENT', 'REFUND', 'QUERY')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED', 'INVALID_AMOUNT', 'INVALID_CARD', 'INSUFFICIENT_BALANCE', 'LIMIT_EXCEEDED', 'BANK_MAINTENANCE', 'INVALID_OTP', 'TIMEOUT', 'DUPLICATE_TRANSACTION', 'BANK_REJECTED', 'INVALID_SIGNATURE', 'UNKNOWN_ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Generic payment transactions table for multi-provider payment gateway integration';

-- 2. VNPay-specific payment details table
CREATE TABLE vnpay_payment_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    vnp_txn_ref VARCHAR(100) NOT NULL,
    vnp_order_info VARCHAR(500),
    vnp_amount BIGINT NOT NULL,
    vnp_order_type VARCHAR(20),
    vnp_locale VARCHAR(5) DEFAULT 'vn',
    vnp_bank_code VARCHAR(20),
    vnp_bank_tran_no VARCHAR(100),
    vnp_card_type VARCHAR(20),
    vnp_pay_date VARCHAR(14),
    vnp_response_code VARCHAR(10),
    vnp_tmn_code VARCHAR(20),
    vnp_transaction_no VARCHAR(100),
    vnp_transaction_status VARCHAR(10),
    vnp_secure_hash VARCHAR(256),
    vnp_secure_hash_type VARCHAR(10) DEFAULT 'SHA512',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_vnpay_payment_id (payment_id),
    INDEX idx_vnpay_txn_ref (vnp_txn_ref),
    INDEX idx_vnpay_transaction_no (vnp_transaction_no),
    INDEX idx_vnpay_response_code (vnp_response_code),

    CONSTRAINT fk_vnpay_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT chk_vnpay_amount CHECK (vnp_amount > 0),
    CONSTRAINT chk_vnpay_locale CHECK (vnp_locale IN ('vn', 'en')),
    CONSTRAINT chk_vnpay_response_code CHECK (vnp_response_code IN ('00', '07', '09', '10', '11', '12', '13', '24', '51', '65', '75', '79', '99'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VNPay-specific payment details and response data';

