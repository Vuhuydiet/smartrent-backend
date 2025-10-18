-- =====================================================
-- SMARTRENT MEMBERSHIP & TRANSACTION SYSTEM
-- Version: V13
-- Description: Complete membership packages, benefits, transactions, and boost system
-- =====================================================

-- =====================================================
-- 1. MEMBERSHIP PACKAGES TABLE
-- =====================================================
CREATE TABLE membership_packages (
    membership_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    package_code VARCHAR(50) NOT NULL UNIQUE,
    package_name VARCHAR(100) NOT NULL,
    package_level ENUM('BASIC', 'STANDARD', 'ADVANCED') NOT NULL,
    duration_months INT NOT NULL DEFAULT 1,
    original_price DECIMAL(15, 0) NOT NULL,
    sale_price DECIMAL(15, 0) NOT NULL,
    discount_percentage DECIMAL(5, 2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_package_level (package_level),
    INDEX idx_is_active (is_active),
    INDEX idx_package_code (package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. MEMBERSHIP PACKAGE BENEFITS TABLE
-- =====================================================
CREATE TABLE membership_package_benefits (
    benefit_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    membership_id BIGINT NOT NULL,
    benefit_type ENUM('VIP_POSTS', 'PREMIUM_POSTS', 'BOOST_QUOTA', 'AUTO_VERIFY', 'TRUSTED_BADGE') NOT NULL,
    benefit_name_display VARCHAR(200) NOT NULL,
    quantity_per_month INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mpb_membership FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id) ON DELETE CASCADE,
    INDEX idx_membership_id (membership_id),
    INDEX idx_benefit_type (benefit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. USER MEMBERSHIPS TABLE
-- =====================================================
CREATE TABLE user_memberships (
    user_membership_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    membership_id BIGINT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    duration_days INT NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    total_paid DECIMAL(15, 0) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_um_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_um_membership FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_end_date (end_date),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 4. USER MEMBERSHIP BENEFITS TABLE
-- =====================================================
CREATE TABLE user_membership_benefits (
    user_benefit_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_membership_id BIGINT NOT NULL,
    benefit_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    benefit_type ENUM('VIP_POSTS', 'PREMIUM_POSTS', 'BOOST_QUOTA', 'AUTO_VERIFY', 'TRUSTED_BADGE') NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    total_quantity INT NOT NULL,
    quantity_used INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'FULLY_USED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_umb_user_membership FOREIGN KEY (user_membership_id) REFERENCES user_memberships(user_membership_id) ON DELETE CASCADE,
    CONSTRAINT fk_umb_benefit FOREIGN KEY (benefit_id) REFERENCES membership_package_benefits(benefit_id),
    CONSTRAINT fk_umb_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_benefit_type (benefit_type),
    INDEX idx_status (status),
    INDEX idx_user_benefit_status (user_id, benefit_type, status),
    INDEX idx_expires_at (expires_at),
    UNIQUE KEY uk_user_membership_benefit (user_membership_id, benefit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 5. TRANSACTIONS TABLE
-- =====================================================
CREATE TABLE transactions (
    transaction_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'BOOST_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL,
    amount DECIMAL(15, 0) NOT NULL,
    balance_before DECIMAL(15, 0),
    balance_after DECIMAL(15, 0),
    reference_type ENUM('MEMBERSHIP', 'LISTING', 'BOOST', 'WALLET') NOT NULL,
    reference_id VARCHAR(100),
    additional_info TEXT,
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    payment_provider ENUM('VNPAY', 'MOMO', 'WALLET', 'BANK_TRANSFER', 'CASH') DEFAULT 'VNPAY',
    provider_transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_status (status),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_created_at (created_at),
    INDEX idx_provider_tx_id (provider_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 6. PUSH SCHEDULE TABLE
-- =====================================================
CREATE TABLE push_schedule (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    listing_id BIGINT NOT NULL,
    scheduled_time TIME NOT NULL,
    source ENUM('MEMBERSHIP', 'DIRECT_PURCHASE') NOT NULL,
    source_id BIGINT,
    total_pushes INT NOT NULL DEFAULT 1,
    used_pushes INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    transaction_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ps_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    INDEX idx_listing_id (listing_id),
    INDEX idx_status (status),
    INDEX idx_scheduled_time (scheduled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 7. PUSH HISTORY TABLE
-- =====================================================
CREATE TABLE push_history (
    push_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    push_source ENUM('MEMBERSHIP_QUOTA', 'DIRECT_PURCHASE', 'SCHEDULED', 'ADMIN', 'DIRECT_PAYMENT') NOT NULL,
    user_benefit_id BIGINT,
    schedule_id BIGINT,
    transaction_id VARCHAR(36),
    status ENUM('SUCCESS', 'FAIL') DEFAULT 'SUCCESS',
    message VARCHAR(500),
    pushed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ph_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE,
    CONSTRAINT fk_ph_benefit FOREIGN KEY (user_benefit_id) REFERENCES user_membership_benefits(user_benefit_id) ON DELETE SET NULL,
    CONSTRAINT fk_ph_schedule FOREIGN KEY (schedule_id) REFERENCES push_schedule(schedule_id) ON DELETE SET NULL,
    CONSTRAINT fk_ph_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    INDEX idx_listing_id (listing_id),
    INDEX idx_pushed_at (pushed_at),
    INDEX idx_listing_pushed (listing_id, pushed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 8. UPDATE LISTINGS TABLE - Add shadow listing support
-- =====================================================
ALTER TABLE listings
ADD COLUMN is_shadow BOOLEAN NOT NULL DEFAULT FALSE AFTER vip_type,
ADD COLUMN parent_listing_id BIGINT AFTER is_shadow,
ADD COLUMN pushed_at TIMESTAMP AFTER post_date,
ADD INDEX idx_parent_listing (parent_listing_id),
ADD INDEX idx_is_shadow (is_shadow),
ADD INDEX idx_pushed_at (pushed_at),
ADD CONSTRAINT fk_listing_parent FOREIGN KEY (parent_listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE;

-- =====================================================
-- 9. INSERT DEFAULT MEMBERSHIP PACKAGES
-- =====================================================
INSERT INTO membership_packages (package_code, package_name, package_level, duration_months, original_price, sale_price, discount_percentage, description) VALUES
('BASIC_1M', 'Gói Cơ Bản 1 Tháng', 'BASIC', 1, 1000000, 700000, 30.00, 'Gói cơ bản cho người dùng mới bắt đầu'),
('STANDARD_1M', 'Gói Tiêu Chuẩn 1 Tháng', 'STANDARD', 1, 2000000, 1400000, 30.00, 'Gói tiêu chuẩn với nhiều tính năng hơn'),
('ADVANCED_1M', 'Gói Nâng Cao 1 Tháng', 'ADVANCED', 1, 4000000, 2800000, 30.00, 'Gói cao cấp với đầy đủ tính năng');

-- =====================================================
-- 10. INSERT MEMBERSHIP PACKAGE BENEFITS
-- =====================================================
-- BASIC Package Benefits
INSERT INTO membership_package_benefits (membership_id, benefit_type, benefit_name_display, quantity_per_month) VALUES
(1, 'VIP_POSTS', '5 tin VIP miễn phí', 5),
(1, 'BOOST_QUOTA', '10 lượt đẩy tin miễn phí', 10);

-- STANDARD Package Benefits
INSERT INTO membership_package_benefits (membership_id, benefit_type, benefit_name_display, quantity_per_month) VALUES
(2, 'VIP_POSTS', '10 tin VIP miễn phí', 10),
(2, 'PREMIUM_POSTS', '5 tin Premium miễn phí', 5),
(2, 'BOOST_QUOTA', '20 lượt đẩy tin miễn phí', 20),
(2, 'AUTO_VERIFY', 'Duyệt tin ngay lập tức', 1);

-- ADVANCED Package Benefits
INSERT INTO membership_package_benefits (membership_id, benefit_type, benefit_name_display, quantity_per_month) VALUES
(3, 'VIP_POSTS', '15 tin VIP miễn phí', 15),
(3, 'PREMIUM_POSTS', '10 tin Premium miễn phí', 10),
(3, 'BOOST_QUOTA', '40 lượt đẩy tin miễn phí', 40),
(3, 'AUTO_VERIFY', 'Duyệt tin ngay lập tức', 1),
(3, 'TRUSTED_BADGE', 'Badge đối tác tin cậy', 1);

-- =====================================================
-- END OF MIGRATION V13
-- =====================================================

