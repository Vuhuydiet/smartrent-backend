-- Add report status and admin resolution fields to listing_reports table
-- This migration adds fields to track admin review and resolution of reports

-- Add status column with default PENDING
ALTER TABLE listing_reports
ADD COLUMN status ENUM('PENDING', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER category;

-- Add resolved_by column to track which admin resolved the report
ALTER TABLE listing_reports
ADD COLUMN resolved_by VARCHAR(36) NULL AFTER status;

-- Add resolved_at timestamp
ALTER TABLE listing_reports
ADD COLUMN resolved_at TIMESTAMP NULL AFTER resolved_by;

-- Add admin_notes for admin comments
ALTER TABLE listing_reports
ADD COLUMN admin_notes TEXT NULL AFTER resolved_at;

-- Add updated_at timestamp
ALTER TABLE listing_reports
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER admin_notes;

-- Add foreign key constraint to admins table
ALTER TABLE listing_reports
ADD CONSTRAINT fk_listing_reports_admin
FOREIGN KEY (resolved_by) REFERENCES admins(admin_id) ON DELETE SET NULL;

-- Add indexes for efficient querying
CREATE INDEX idx_status ON listing_reports(status);
CREATE INDEX idx_resolved_by ON listing_reports(resolved_by);

