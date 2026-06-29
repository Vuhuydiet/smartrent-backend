-- Add MEMBERSHIP_RENEWAL to transaction_type ENUM
-- This was missing, causing "Data truncated for column 'transaction_type'" on renewal.

ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM(
    'MEMBERSHIP_PURCHASE',
    'MEMBERSHIP_UPGRADE',
    'MEMBERSHIP_RENEWAL',
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
