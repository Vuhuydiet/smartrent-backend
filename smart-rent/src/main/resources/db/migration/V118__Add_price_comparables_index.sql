-- Index supporting the price-comparables aggregate endpoint
-- (POST /v1/listings/price-comparables).
--
-- The query filters an exact (listing_type, product_type, price_unit) segment
-- down to only publicly-visible, verified, non-expired rows, then range-scans a
-- latitude bounding box and refines by Haversine distance. Leading equality
-- columns first, latitude as the single range column, then longitude/area/price
-- as a covering suffix so the aggregate reads price+area straight from the index
-- without a table lookup — same philosophy as idx_listings_map_bounds.
CREATE INDEX idx_listings_price_comps
    ON listings (listing_type, product_type, price_unit,
                 is_shadow, is_draft, verified, expired,
                 latitude, longitude, area, price);
