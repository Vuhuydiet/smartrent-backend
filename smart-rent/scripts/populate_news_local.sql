-- ============================================================
-- Populate 50 news rows for local development
-- ============================================================

DROP PROCEDURE IF EXISTS populate_news_local;

DELIMITER $$

CREATE PROCEDURE populate_news_local()
BEGIN
    DECLARE i      INT DEFAULT 1;
    DECLARE cat    VARCHAR(20);
    DECLARE stat   VARCHAR(10);
    DECLARE pub_at DATETIME;
    DECLARE v_title VARCHAR(255);
    DECLARE v_slug  VARCHAR(300);

    WHILE i <= 50 DO

        SET cat = ELT(1 + (i % 7),
            'NEWS', 'MARKET', 'POLICY', 'BLOG', 'INVESTMENT', 'PROJECT', 'GUIDE'
        );

        SET stat = CASE
            WHEN (i % 10) = 0 THEN 'DRAFT'
            WHEN (i % 7)  = 0 THEN 'ARCHIVED'
            ELSE 'PUBLISHED'
        END;

        SET pub_at = CASE
            WHEN stat = 'DRAFT' THEN NULL
            ELSE DATE_ADD('2024-06-01', INTERVAL (i * 14) % 670 DAY)
        END;

        SET v_title = CONCAT(
            ELT(1 + (i % 7),
                'Thị trường bất động sản',
                'Phân tích thị trường BĐS',
                'Chính sách nhà ở mới',
                'Kinh nghiệm mua nhà',
                'Đầu tư bất động sản',
                'Dự án căn hộ mới',
                'Hướng dẫn mua nhà'
            ),
            ' - Bài viết số ', i
        );

        SET v_slug = CONCAT('local-news-', i, '-', UNIX_TIMESTAMP());

        INSERT IGNORE INTO news (
            title, slug, summary, content, category,
            tags, thumbnail_url, status, published_at,
            author_name, view_count,
            meta_title, meta_description, meta_keywords
        ) VALUES (
            v_title,
            v_slug,
            CONCAT('Tóm tắt bài viết số ', i, ' về ', cat, '. Nội dung cập nhật mới nhất về thị trường bất động sản Việt Nam.'),
            CONCAT(
                '<h2>', v_title, '</h2>',
                '<p>Đây là nội dung bài viết số ', i, ' thuộc danh mục ', cat, '.</p>',
                '<p>Thị trường bất động sản Việt Nam đang có nhiều diễn biến tích cực. Các chuyên gia nhận định đây là thời điểm phù hợp để đầu tư vào phân khúc nhà ở trung cấp.</p>',
                '<p>Người mua nhà cần chú ý kiểm tra pháp lý kỹ trước khi giao dịch.</p>'
            ),
            cat,
            'bất động sản,nhà đất,tin tức',
            'https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/news-image/Anh-1-Anh-cover-scaled.jpg',
            stat,
            pub_at,
            ELT(1 + (i % 5), 'Nguyễn Minh Tuấn', 'Trần Thị Lan', 'Lê Văn Hùng', 'Phạm Thị Mai', 'Hoàng Anh Dũng'),
            FLOOR(100 + RAND() * 4900),
            LEFT(CONCAT(v_title, ' | SmartRent'), 255),
            LEFT(CONCAT('Tóm tắt bài viết số ', i, ' về ', cat), 500),
            'bất động sản, nhà đất, tin tức BĐS'
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL populate_news_local();
DROP PROCEDURE IF EXISTS populate_news_local;

SELECT category, status, COUNT(*) AS cnt
FROM news
GROUP BY category, status
ORDER BY category, status;
