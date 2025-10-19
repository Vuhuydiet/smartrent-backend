-- V18__Import_tinhthanhpho_location_data.sql
-- Migration to import Vietnamese administrative location data from tinhthanhpho.com
--
-- INSTRUCTIONS:
-- 1. Fetch data from tinhthanhpho.com API endpoints:
--    - GET https://tinhthanhpho.com/api/tinh-thanh-pho (Provinces)
--    - GET https://tinhthanhpho.com/api/quan-huyen/tinh-thanh-pho/{provinceId} (Districts)
--    - GET https://tinhthanhpho.com/api/phuong-xa/quan-huyen/{districtId} (Wards)
--
-- 2. Convert JSON responses to SQL INSERT statements
-- 3. Replace the template INSERTs below with actual data
--
-- NOTE: This is a TEMPLATE. You need to populate it with actual data from the API.
--       See docs/TINHTHANHPHO_DATA_IMPORT_GUIDE.md for detailed instructions.

-- =====================================================================
-- SECTION 1: Import Provinces (Tỉnh/Thành phố)
-- =====================================================================
-- Template format:
-- INSERT INTO provinces (name, code, type, is_active, effective_from, is_merged)
-- VALUES ('Thành phố Hà Nội', '01', 'CITY', true, CURRENT_DATE, false);

-- Example provinces (replace with actual data from API):
INSERT INTO provinces (name, code, type, is_active, effective_from, is_merged) VALUES
('Thành phố Hà Nội', '01', 'CITY', true, CURRENT_DATE, false),
('Thành phố Hồ Chí Minh', '79', 'CITY', true, CURRENT_DATE, false),
('Thành phố Đà Nẵng', '48', 'CITY', true, CURRENT_DATE, false),
('Thành phố Hải Phòng', '31', 'CITY', true, CURRENT_DATE, false),
('Thành phố Cần Thơ', '92', 'CITY', true, CURRENT_DATE, false)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- TODO: Add remaining 58 provinces from tinhthanhpho.com API
-- Total: 63 provinces (5 cities + 58 provinces)


-- =====================================================================
-- SECTION 2: Import Districts (Quận/Huyện)
-- =====================================================================
-- Template format:
-- INSERT INTO districts (name, code, type, province_id, is_active, effective_from)
-- SELECT 'Quận Ba Đình', '001', 'DISTRICT', province_id, true, CURRENT_DATE
-- FROM provinces WHERE code = '01';

-- Example districts for Hanoi (replace with actual data from API):
INSERT INTO districts (name, code, type, province_id, is_active, effective_from)
SELECT 'Quận Ba Đình', '001', 'DISTRICT', province_id, true, CURRENT_DATE FROM provinces WHERE code = '01' UNION ALL
SELECT 'Quận Hoàn Kiếm', '002', 'DISTRICT', province_id, true, CURRENT_DATE FROM provinces WHERE code = '01' UNION ALL
SELECT 'Quận Tây Hồ', '003', 'DISTRICT', province_id, true, CURRENT_DATE FROM provinces WHERE code = '01' UNION ALL
SELECT 'Quận Long Biên', '004', 'DISTRICT', province_id, true, CURRENT_DATE FROM provinces WHERE code = '01' UNION ALL
SELECT 'Quận Cầu Giấy', '005', 'DISTRICT', province_id, true, CURRENT_DATE FROM provinces WHERE code = '01'
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- TODO: Add all districts from tinhthanhpho.com API
-- Total: Approximately 700+ districts across all provinces


-- =====================================================================
-- SECTION 3: Import Wards (Phường/Xã)
-- =====================================================================
-- Template format:
-- INSERT INTO wards (name, code, type, district_id, is_active, effective_from)
-- SELECT 'Phường Phúc Xá', '00001', 'WARD', district_id, true, CURRENT_DATE
-- FROM districts WHERE code = '001';

-- Example wards for Ba Dinh district (replace with actual data from API):
INSERT INTO wards (name, code, type, district_id, is_active, effective_from)
SELECT 'Phường Phúc Xá', '00001', 'WARD', district_id, true, CURRENT_DATE FROM districts WHERE code = '001' UNION ALL
SELECT 'Phường Trúc Bạch', '00002', 'WARD', district_id, true, CURRENT_DATE FROM districts WHERE code = '001' UNION ALL
SELECT 'Phường Vĩnh Phúc', '00003', 'WARD', district_id, true, CURRENT_DATE FROM districts WHERE code = '001' UNION ALL
SELECT 'Phường Cống Vị', '00004', 'WARD', district_id, true, CURRENT_DATE FROM districts WHERE code = '001' UNION ALL
SELECT 'Phường Liễu Giai', '00005', 'WARD', district_id, true, CURRENT_DATE FROM districts WHERE code = '001'
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- TODO: Add all wards from tinhthanhpho.com API
-- Total: Approximately 11,000+ wards across all districts


-- =====================================================================
-- VERIFICATION QUERIES (Run after import)
-- =====================================================================
-- Check counts:
-- SELECT COUNT(*) as total_provinces FROM provinces WHERE is_active = true;
-- SELECT COUNT(*) as total_districts FROM districts WHERE is_active = true;
-- SELECT COUNT(*) as total_wards FROM wards WHERE is_active = true;
--
-- Expected results:
-- - Provinces: 63
-- - Districts: ~700+
-- - Wards: ~11,000+

-- Check data integrity:
-- SELECT p.name as province, COUNT(d.district_id) as district_count
-- FROM provinces p
-- LEFT JOIN districts d ON p.province_id = d.province_id AND d.is_active = true
-- WHERE p.is_active = true
-- GROUP BY p.province_id, p.name
-- ORDER BY p.name;