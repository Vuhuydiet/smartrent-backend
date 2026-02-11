-- Create report_reasons table
CREATE TABLE IF NOT EXISTS report_reasons (
    reason_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reason_text VARCHAR(500) NOT NULL,
    category ENUM('LISTING', 'MAP') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create listing_reports table
CREATE TABLE IF NOT EXISTS listing_reports (
    report_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    reporter_name VARCHAR(200),
    reporter_phone VARCHAR(20) NOT NULL,
    reporter_email VARCHAR(255) NOT NULL,
    other_feedback TEXT,
    category ENUM('LISTING', 'MAP') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_listing_id (listing_id),
    INDEX idx_reporter_email (reporter_email),
    INDEX idx_reporter_phone (reporter_phone),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create listing_report_reasons junction table
CREATE TABLE IF NOT EXISTS listing_report_reasons (
    report_id BIGINT NOT NULL,
    reason_id BIGINT NOT NULL,
    PRIMARY KEY (report_id, reason_id),
    FOREIGN KEY (report_id) REFERENCES listing_reports(report_id) ON DELETE CASCADE,
    FOREIGN KEY (reason_id) REFERENCES report_reasons(reason_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert predefined report reasons for LISTING category
INSERT INTO report_reasons (reason_text, category, display_order) VALUES
('Các thông tin về: giá, diện tích, mô tả', 'LISTING', 1),
('Ảnh', 'LISTING', 2),
('Trùng với tin rao khác', 'LISTING', 3),
('Không liên lạc được', 'LISTING', 4),
('Tin không có thật', 'LISTING', 5),
('Bất động sản đã bán', 'LISTING', 6),
('Địa chỉ của bất động sản', 'LISTING', 7);

-- Insert predefined report reasons for MAP category
INSERT INTO report_reasons (reason_text, category, display_order) VALUES
('Vị trí bất động sản chưa chính xác', 'MAP', 1),
('Vị trí tiện ích chưa chính xác', 'MAP', 2),
('Bản đồ lỗi', 'MAP', 3),
('Tốc độ load chậm', 'MAP', 4);

