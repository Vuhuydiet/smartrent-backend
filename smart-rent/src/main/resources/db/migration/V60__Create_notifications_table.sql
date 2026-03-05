-- Notifications table for in-app realtime notifications
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id    VARCHAR(36)  NOT NULL COMMENT 'userId or adminId',
    recipient_type  VARCHAR(10)  NOT NULL COMMENT 'USER or ADMIN',
    type            VARCHAR(50)  NOT NULL COMMENT 'NotificationType enum value',
    title           VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    reference_id    BIGINT                COMMENT 'listing_id or report_id',
    reference_type  VARCHAR(30)           COMMENT 'LISTING or REPORT',
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_notif_recipient     (recipient_id, recipient_type),
    INDEX idx_notif_unread        (recipient_id, is_read),
    INDEX idx_notif_created       (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
