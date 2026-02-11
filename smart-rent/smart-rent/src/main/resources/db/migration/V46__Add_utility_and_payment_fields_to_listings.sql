-- Add utility costs and payment-related fields to listings table
-- These fields support the new listing creation flow with detailed pricing and payment information

-- Add utility cost fields
ALTER TABLE listings
ADD COLUMN water_price VARCHAR(50),
ADD COLUMN electricity_price VARCHAR(50),
ADD COLUMN internet_price VARCHAR(50),
ADD COLUMN service_fee VARCHAR(50);

-- Add payment and duration fields
ALTER TABLE listings
ADD COLUMN duration_days INT DEFAULT 30,
ADD COLUMN use_membership_quota BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN payment_provider VARCHAR(50);

-- Add comments for clarity
ALTER TABLE listings
MODIFY COLUMN water_price VARCHAR(50) COMMENT 'Water pricing information (e.g., "50000 VND/month", "Included in rent")',
MODIFY COLUMN electricity_price VARCHAR(50) COMMENT 'Electricity pricing information (e.g., "3500 VND/kWh", "Metered")',
MODIFY COLUMN internet_price VARCHAR(50) COMMENT 'Internet pricing information (e.g., "100000 VND/month", "Free WiFi")',
MODIFY COLUMN service_fee VARCHAR(50) COMMENT 'Service fee information (e.g., "200000 VND/month", "None")',
MODIFY COLUMN duration_days INT DEFAULT 30 COMMENT 'Duration of the listing in days (default: 30)',
MODIFY COLUMN use_membership_quota BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this listing uses membership quota (VIP) or direct payment',
MODIFY COLUMN payment_provider VARCHAR(50) COMMENT 'Payment provider used (e.g., VNPAY, MOMO)';