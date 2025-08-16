-- Insert default admin user
-- This migration creates a default admin user for initial system access
-- Password is 'admin123' (should be changed after first login)

INSERT INTO users (
    id,
    username,
    password
) VALUES (
    'admin-001',
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyEOoM5N6jm' -- BCrypt hash for 'admin123'
) ON DUPLICATE KEY UPDATE
    password = VALUES(password);

-- Note: In production, this user should be created with a secure password
-- and the default password should be changed immediately after deployment
