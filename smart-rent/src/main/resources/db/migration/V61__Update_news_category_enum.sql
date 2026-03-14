-- V61: Update news category enum to include new real estate categories
-- Add POLICY, MARKET, PROJECT, INVESTMENT, GUIDE categories

ALTER TABLE news
MODIFY COLUMN category ENUM('NEWS', 'BLOG', 'POLICY', 'MARKET', 'PROJECT', 'INVESTMENT', 'GUIDE') NOT NULL;
