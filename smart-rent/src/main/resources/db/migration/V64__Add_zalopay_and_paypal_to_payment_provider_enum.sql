-- Add ZALOPAY to payment_provider ENUM in transactions table
ALTER TABLE transactions 
MODIFY COLUMN payment_provider ENUM('VNPAY', 'MOMO', 'WALLET', 'BANK_TRANSFER', 'CASH', 'ZALOPAY', 'PAYPAL') DEFAULT 'VNPAY';
