-- Admin-owned media (e.g. news editor images) cannot satisfy fk_media_user, since admins
-- live in a separate `admins` table, not `users`. Add a dedicated admin_id column so admin
-- uploads no longer need to be forced through the user FK.
ALTER TABLE media
    ADD COLUMN admin_id VARCHAR(36) NULL COMMENT 'Owner admin ID (mutually exclusive with user_id)' AFTER user_id,
    MODIFY COLUMN user_id VARCHAR(36) NULL COMMENT 'Owner user ID (mutually exclusive with admin_id)';

ALTER TABLE media
    ADD CONSTRAINT fk_media_admin
        FOREIGN KEY (admin_id)
        REFERENCES admins(admin_id)
        ON DELETE CASCADE,
    ADD CONSTRAINT chk_media_owner
        CHECK (
            (user_id IS NOT NULL AND admin_id IS NULL) OR
            (user_id IS NULL AND admin_id IS NOT NULL)
        );

CREATE INDEX idx_media_admin_id ON media (admin_id);
