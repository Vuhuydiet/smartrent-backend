-- 1. Chèn Address mẫu (nếu chưa có)
INSERT INTO addresses (address_id, city, district, ward, street, full_address, display_address)
VALUES (999, 'TP. Hồ Chí Minh', 'Quận Bình Thạnh', 'Phường 22', '208 Nguyễn Hữu Cảnh', '208 Nguyễn Hữu Cảnh, Phường 22, Quận Bình Thạnh, TP. Hồ Chí Minh', 'Vinhomes Central Park');

-- 2. Chèn Listing mẫu 1: Cần AI duyệt (Video hoạt hình)
INSERT INTO listings (listing_id, title, description, price, area, address_id, user_id, listing_type, product_type, verified, is_verify, expired, created_at, updated_at)
VALUES (9991, 'Căn hộ Test AI - Video Hoạt Hình', 'Mô tả có video thực tế nhưng thực chất là phim hoạt hình.', 15000000, 75.0, 999, 'user-test-id', 'RENT', 'APARTMENT', 0, 1, 0, NOW(), NOW());

INSERT INTO media (listing_id, user_id, media_type, source_type, status, url, created_at)
VALUES (9991, 'user-test-id', 'IMAGE', 'EXTERNAL', 'ACTIVE', 'https://images.pexels.com/photos/1571460/pexels-photo-1571460.jpeg', NOW());

INSERT INTO media (listing_id, user_id, media_type, source_type, status, url, created_at)
VALUES (9991, 'user-test-id', 'VIDEO', 'EXTERNAL', 'ACTIVE', 'https://www.w3schools.com/html/mov_bbb.mp4', NOW());


-- 3. Chèn Listing mẫu 2: Cần AI duyệt (Ảnh mâu thuẫn - Căn hộ nhưng ảnh nhà vườn)
INSERT INTO listings (listing_id, title, description, price, area, address_id, user_id, listing_type, product_type, verified, is_verify, expired, created_at, updated_at)
VALUES (9992, 'Căn hộ chung cư tầng 35', 'Căn hộ chung cư cao cấp nhưng ảnh đính kèm là nhà vườn.', 25000000, 50.0, 999, 'user-test-id', 'RENT', 'APARTMENT', 0, 1, 0, NOW(), NOW());

INSERT INTO media (listing_id, user_id, media_type, source_type, status, url, created_at)
VALUES (9992, 'user-test-id', 'IMAGE', 'EXTERNAL', 'ACTIVE', 'https://images.pexels.com/photos/106399/pexels-photo-106399.jpeg', NOW());

-- 4. Chèn sẵn bản ghi vào bảng listing_ai_moderation để Scheduler nhận biết (nếu repo yêu cầu)
INSERT INTO listing_ai_moderation (listing_id, verification_status, ai_score, retry_count, manual_override, created_at)
VALUES (9991, 'PENDING', 0.0, 0, 0, NOW());

INSERT INTO listing_ai_moderation (listing_id, verification_status, ai_score, retry_count, manual_override, created_at)
VALUES (9992, 'PENDING', 0.0, 0, 0, NOW());
