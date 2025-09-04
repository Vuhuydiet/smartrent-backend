CREATE TABLE invalidated_tokens (
    access_id VARCHAR(255) NOT NULL PRIMARY KEY,
    refresh_id VARCHAR(255),
    expiration_time TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invalidated_tokens_refresh_id ON invalidated_tokens (refresh_id);
CREATE INDEX idx_invalidated_tokens_expiration_time ON invalidated_tokens (expiration_time);
