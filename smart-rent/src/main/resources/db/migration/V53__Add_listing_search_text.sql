-- Add lightweight search fields for listings
ALTER TABLE listings
    ADD COLUMN search_text VARCHAR(512) NULL,
    ADD COLUMN title_norm VARCHAR(256) NULL;

-- Indexes for public search and autocomplete
CREATE INDEX idx_listings_public_search
    ON listings (is_shadow, is_draft, verified, expired, vip_type_sort_order, updated_at);

CREATE INDEX idx_listings_user_drafts
    ON listings (user_id, is_draft, updated_at);

CREATE INDEX idx_listings_search_text
    ON listings (search_text(128));

CREATE INDEX idx_listings_title_norm
    ON listings (title_norm(64));
