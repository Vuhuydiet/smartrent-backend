-- Switch the default payment gateway to SePay.
-- SEPAY is added to the payment_provider ENUM and becomes the new default.
-- Legacy values (PAYOS, VNPAY, etc.) are kept in the ENUM definition so historical rows are preserved.
ALTER TABLE transactions
MODIFY COLUMN payment_provider
    ENUM('SEPAY', 'PAYOS', 'VNPAY', 'MOMO', 'WALLET', 'BANK_TRANSFER', 'CASH', 'ZALOPAY', 'PAYPAL')
    DEFAULT 'SEPAY';
