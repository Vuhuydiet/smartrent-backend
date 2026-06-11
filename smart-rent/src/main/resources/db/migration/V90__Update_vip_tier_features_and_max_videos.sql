-- =====================================================
-- SMARTRENT VIP TIER FEATURES & MAX VIDEOS UPDATE
-- Version: V90
-- Description: Refresh the `features` JSON descriptions for each VIP tier and
--              cap max_videos at 1 video for every tier to match the new spec.
-- =====================================================

-- NORMAL Tier
UPDATE vip_tier_details
SET max_videos = 1,
    features = JSON_ARRAY(
        'Hiển thị theo thứ tự thời gian',
        'Tối đa 5 ảnh, 1 video',
        'Hiển thị dưới cùng'
    )
WHERE tier_code = 'NORMAL';

-- SILVER Tier (VIP Bạc)
UPDATE vip_tier_details
SET max_videos = 1,
    features = JSON_ARRAY(
        'Badge tin ưu tiên',
        'Ưu tiên hiển thị trên tin thường',
        'Tối đa 10 ảnh, 1 video'
    )
WHERE tier_code = 'SILVER';

-- GOLD Tier (VIP Vàng)
UPDATE vip_tier_details
SET max_videos = 1,
    features = JSON_ARRAY(
        'Tất cả tính năng của VIP Bạc',
        'Ưu tiên hiển thị trên VIP Bạc',
        'Tối đa 12 ảnh, 1 video',
        'Hiển thị ở vị trí tốt hơn'
    )
WHERE tier_code = 'GOLD';

-- DIAMOND Tier (VIP Kim Cương)
UPDATE vip_tier_details
SET max_videos = 1,
    features = JSON_ARRAY(
        'Tất cả tính năng của VIP Vàng',
        'Ưu tiên hiển thị CAO NHẤT',
        'Tối đa 15 ảnh, 1 video',
        'Hiển thị vị trí TOP trang chủ'
    )
WHERE tier_code = 'DIAMOND';

-- =====================================================
-- END OF MIGRATION V90
-- =====================================================
