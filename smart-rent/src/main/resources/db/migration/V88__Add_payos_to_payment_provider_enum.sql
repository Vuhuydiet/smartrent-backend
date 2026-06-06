-- Switch the default payment gateway to PayOS.
-- PAYOS is added to the payment_provider ENUM and becomes the new default.
-- Legacy values (VNPAY, etc.) are kept in the ENUM definition so historical rows are preserved.
ALTER TABLE transactions
MODIFY COLUMN payment_provider
    ENUM('PAYOS', 'VNPAY', 'MOMO', 'WALLET', 'BANK_TRANSFER', 'CASH', 'ZALOPAY', 'PAYPAL')
    DEFAULT 'PAYOS';
