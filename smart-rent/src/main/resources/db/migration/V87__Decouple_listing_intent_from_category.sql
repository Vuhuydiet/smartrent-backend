-- Decouple the "intent" axis (listing_type: RENT / SALE / SHARE) from the
-- property-kind axis. Two changes:
--
-- 1. Category names baked the intent into the label ("Cho thuê phòng trọ").
--    The intent now lives in listings.listing_type, so categories become pure
--    property kinds. Slugs are kept unchanged to preserve existing links/SEO;
--    only the display name changes.
--
-- 2. The product_type ENUM had no value for the existing "Mặt bằng" (store)
--    category, while the column is NOT NULL. Add STORE so every category maps
--    1:1 to a product_type and the value can always be derived from category.

-- 1. Rename categories: strip the "Cho thuê " intent prefix.
UPDATE categories SET name = 'Phòng trọ'      WHERE slug = 'cho-thue-phong-tro';
UPDATE categories SET name = 'Căn hộ'         WHERE slug = 'cho-thue-can-ho';
UPDATE categories SET name = 'Nhà nguyên căn' WHERE slug = 'cho-thue-nha-nguyen-can';
UPDATE categories SET name = 'Văn phòng'      WHERE slug = 'cho-thue-van-phong';
UPDATE categories SET name = 'Mặt bằng'       WHERE slug = 'cho-thue-mat-bang';

-- 2. Add STORE to the product_type ENUM (keeps NOT NULL).
ALTER TABLE listings
    MODIFY COLUMN product_type
    ENUM('ROOM', 'APARTMENT', 'HOUSE', 'OFFICE', 'STUDIO', 'STORE') NOT NULL;
