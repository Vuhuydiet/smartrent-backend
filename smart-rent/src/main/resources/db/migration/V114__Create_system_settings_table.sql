-- Migration V114: generic key-value table for admin-configurable settings.
--
-- First consumer: `ai_auto_verify_enabled` — replaces the old in-memory
-- AtomicBoolean (AiListingAutoModerationScheduler) that reset to its
-- config-file default on every server restart. The background cronjob that
-- auto-approved/auto-rejected listings without human review has been
-- removed; this flag now gates whether the admin post-review dialog
-- auto-runs AI analysis on open instead of requiring a manual click.
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_by VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO system_settings (setting_key, setting_value)
SELECT 'ai_auto_verify_enabled', 'true'
WHERE NOT EXISTS (
    SELECT 1 FROM system_settings WHERE setting_key = 'ai_auto_verify_enabled'
);
