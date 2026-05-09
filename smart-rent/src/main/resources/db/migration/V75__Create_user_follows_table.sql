-- User-to-user follow edges. A row means follower_id has subscribed to
-- following_id's activity (currently used to fan out new-listing notifications).
-- Pair is unique so the application layer can rely on insert-or-noop semantics
-- and a single delete for unfollow.

CREATE TABLE IF NOT EXISTS user_follows (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    follower_id  VARCHAR(36)  NOT NULL,
    following_id VARCHAR(36)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_follows_follower_following
        UNIQUE KEY (follower_id, following_id),
    KEY idx_user_follows_following (following_id),
    KEY idx_user_follows_follower (follower_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
