-- Migration script to add user wallet functionality
-- This script creates a separate user_wallets table to manage user credit/wallet system

-- Create user_wallets table
CREATE TABLE user_wallets (
    wallet_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    credit_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_credits_added DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_credits_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_user_wallets_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_wallets_user_id UNIQUE (user_id),
    CONSTRAINT chk_credit_balance_non_negative CHECK (credit_balance >= 0),
    CONSTRAINT chk_total_credits_added_non_negative CHECK (total_credits_added >= 0),
    CONSTRAINT chk_total_credits_spent_non_negative CHECK (total_credits_spent >= 0)
);

-- Create indexes for better performance
CREATE INDEX idx_user_wallets_user_id ON user_wallets(user_id);
CREATE INDEX idx_user_wallets_credit_balance ON user_wallets(credit_balance);
CREATE INDEX idx_user_wallets_currency ON user_wallets(currency);
CREATE INDEX idx_user_wallets_is_active ON user_wallets(is_active);
CREATE INDEX idx_user_wallets_created_at ON user_wallets(created_at);
CREATE INDEX idx_user_wallets_updated_at ON user_wallets(updated_at);

-- Create wallet transactions table for detailed transaction history
CREATE TABLE wallet_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    transaction_type ENUM('CREDIT_ADD', 'CREDIT_SUBTRACT', 'PAYMENT_CREDIT', 'REFUND_CREDIT', 'ADJUSTMENT') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    reference_type ENUM('PAYMENT', 'MANUAL', 'SYSTEM', 'REFUND') NOT NULL DEFAULT 'MANUAL',
    reference_id VARCHAR(255),
    description TEXT,
    reason VARCHAR(500),
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    -- Constraints
    CONSTRAINT fk_wallet_transactions_wallet_id FOREIGN KEY (wallet_id) REFERENCES user_wallets(wallet_id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_transactions_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_balance_before_non_negative CHECK (balance_before >= 0),
    CONSTRAINT chk_balance_after_non_negative CHECK (balance_after >= 0)
);

-- Create indexes for wallet transactions
CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_transactions_user_id ON wallet_transactions(user_id);
CREATE INDEX idx_wallet_transactions_type ON wallet_transactions(transaction_type);
CREATE INDEX idx_wallet_transactions_reference ON wallet_transactions(reference_type, reference_id);
CREATE INDEX idx_wallet_transactions_created_at ON wallet_transactions(created_at);
CREATE INDEX idx_wallet_transactions_amount ON wallet_transactions(amount);

-- Initialize wallets for existing users
INSERT INTO user_wallets (user_id, credit_balance, total_credits_added, total_credits_spent, currency, is_active, created_at, updated_at)
SELECT
    user_id,
    0.00,
    0.00,
    0.00,
    'VND',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM user_wallets WHERE user_wallets.user_id = users.user_id
);

-- Add comments for documentation
ALTER TABLE user_wallets
COMMENT = 'Table to manage user wallet and credit balance information';

ALTER TABLE user_wallets
MODIFY COLUMN wallet_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique wallet identifier',
MODIFY COLUMN user_id VARCHAR(255) NOT NULL COMMENT 'Reference to user ID from users table',
MODIFY COLUMN credit_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Current credit balance in wallet',
MODIFY COLUMN total_credits_added DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Lifetime total credits added to wallet',
MODIFY COLUMN total_credits_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Lifetime total credits spent from wallet',
MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND' COMMENT 'Currency code for wallet balance',
MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the wallet is active',
MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Wallet creation timestamp',
MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last wallet update timestamp';

ALTER TABLE wallet_transactions
COMMENT = 'Table to track all wallet transaction history';

ALTER TABLE wallet_transactions
MODIFY COLUMN transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique transaction identifier',
MODIFY COLUMN wallet_id BIGINT NOT NULL COMMENT 'Reference to wallet ID',
MODIFY COLUMN user_id VARCHAR(255) NOT NULL COMMENT 'Reference to user ID for faster queries',
MODIFY COLUMN transaction_type ENUM('CREDIT_ADD', 'CREDIT_SUBTRACT', 'PAYMENT_CREDIT', 'REFUND_CREDIT', 'ADJUSTMENT') NOT NULL COMMENT 'Type of wallet transaction',
MODIFY COLUMN amount DECIMAL(15,2) NOT NULL COMMENT 'Transaction amount',
MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND' COMMENT 'Transaction currency',
MODIFY COLUMN balance_before DECIMAL(15,2) NOT NULL COMMENT 'Wallet balance before transaction',
MODIFY COLUMN balance_after DECIMAL(15,2) NOT NULL COMMENT 'Wallet balance after transaction',
MODIFY COLUMN reference_type ENUM('PAYMENT', 'MANUAL', 'SYSTEM', 'REFUND') NOT NULL DEFAULT 'MANUAL' COMMENT 'Type of reference for transaction',
MODIFY COLUMN reference_id VARCHAR(255) COMMENT 'Reference ID (payment ID, admin ID, etc.)',
MODIFY COLUMN description TEXT COMMENT 'Detailed transaction description',
MODIFY COLUMN reason VARCHAR(500) COMMENT 'Reason for the transaction',
MODIFY COLUMN metadata JSON COMMENT 'Additional transaction metadata',
MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Transaction timestamp',
MODIFY COLUMN created_by VARCHAR(255) COMMENT 'User/system that created the transaction';