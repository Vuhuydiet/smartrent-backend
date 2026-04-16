-- Backfill initial pricing_history rows for listings that have no pricing history yet.
-- Uses the listing's current price/price_unit and created_at as the effective date.
-- Sets is_current = true and change_type = 'INITIAL'.

INSERT INTO pricing_histories (
    listing_id,
    old_price,
    new_price,
    old_price_unit,
    new_price_unit,
    change_type,
    change_percentage,
    change_amount,
    is_current,
    changed_by,
    change_reason,
    changed_at
)
SELECT
    l.listing_id,
    NULL,
    l.price,
    NULL,
    l.price_unit,
    'INITIAL',
    0,
    0,
    TRUE,
    l.user_id,
    'Initial listing price',
    l.created_at
FROM listings l
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_histories ph WHERE ph.listing_id = l.listing_id
);
