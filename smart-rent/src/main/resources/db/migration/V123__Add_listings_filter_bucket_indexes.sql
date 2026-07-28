-- Indexes supporting the dynamic filter-bucket endpoint
-- (POST /v1/listings/filter-options), which counts listings per price/area/
-- bedrooms bucket to drive the public listings sidebar filters.
--
-- Each bucket count runs the same "public visibility" WHERE prefix as every
-- other public listing query (moderation_status/is_shadow/is_draft/expired),
-- then range-filters on ONE of price/area/bedrooms.
--
-- Price is already covered by idx_listings_public_price_sort (moderation_status,
-- is_shadow, is_draft, expired, price) — no new index needed there. area and
-- bedrooms have no equivalent, so add the same shape for both.
CREATE INDEX idx_listings_public_area_filter
    ON listings (moderation_status, is_shadow, is_draft, expired, area);

CREATE INDEX idx_listings_public_bedrooms_filter
    ON listings (moderation_status, is_shadow, is_draft, expired, bedrooms);
