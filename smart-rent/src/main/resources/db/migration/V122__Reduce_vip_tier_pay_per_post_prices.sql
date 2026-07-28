-- =====================================================
-- REDUCE VIP TIER PAY-PER-POST PRICES
-- Version: V122
-- Description: Pay-per-post VIP prices (no membership package) were far above
-- market expectations (e.g. Diamond/30 days = 6,846,000 VND, well over a
-- month's rent for a typical room). Rebase to 0% / 10% / ~16.67% duration
-- discounts (30 days bills for only 25 of the 30 days) so pay-per-post
-- pricing stays proportionate to what a landlord is actually renting out.
-- =====================================================

UPDATE vip_tier_details SET price_per_day = 500,   price_10_days = 5000,   price_15_days = 6750,   price_30_days = 12500  WHERE tier_code = 'NORMAL';
UPDATE vip_tier_details SET price_per_day = 5000,  price_10_days = 50000,  price_15_days = 67500,  price_30_days = 125000 WHERE tier_code = 'SILVER';
UPDATE vip_tier_details SET price_per_day = 12000, price_10_days = 120000, price_15_days = 162000, price_30_days = 300000 WHERE tier_code = 'GOLD';
UPDATE vip_tier_details SET price_per_day = 32000, price_10_days = 320000, price_15_days = 432000, price_30_days = 800000 WHERE tier_code = 'DIAMOND';
