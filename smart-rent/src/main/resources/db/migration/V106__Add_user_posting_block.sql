-- Add posting-block fields to users table
-- Lets admins block a user from creating new listings (đăng tin) when their
-- listings accumulate too many admin-approved (RESOLVED) reports.

ALTER TABLE users
ADD COLUMN posting_blocked BOOLEAN NOT NULL DEFAULT FALSE AFTER is_verified;

ALTER TABLE users
ADD COLUMN posting_blocked_reason VARCHAR(500) NULL AFTER posting_blocked;

ALTER TABLE users
ADD COLUMN posting_blocked_by_admin_id VARCHAR(36) NULL AFTER posting_blocked_reason;

ALTER TABLE users
ADD COLUMN posting_blocked_at TIMESTAMP NULL AFTER posting_blocked_by_admin_id;

-- Track which admin performed the block (nullable, cleared admin -> keep row)
ALTER TABLE users
ADD CONSTRAINT fk_users_posting_blocked_admin
FOREIGN KEY (posting_blocked_by_admin_id) REFERENCES admins(admin_id) ON DELETE SET NULL;

-- Index to quickly filter blocked users
CREATE INDEX idx_users_posting_blocked ON users(posting_blocked);
