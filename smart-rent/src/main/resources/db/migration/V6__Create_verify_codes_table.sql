CREATE TABLE verify_codes (
    verify_code VARCHAR(6) NOT NULL PRIMARY KEY,
    expiration_time TIMESTAMP NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_verify_codes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for performance
CREATE INDEX idx_verify_codes_user_id ON verify_codes (user_id);
CREATE INDEX idx_verify_codes_expiration_time ON verify_codes (expiration_time);
