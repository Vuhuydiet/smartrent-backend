CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) NOT NULL PRIMARY KEY,
    phone_code VARCHAR(10),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    id_document VARCHAR(50),
    tax_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_phone UNIQUE (phone_code, phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    role_id VARCHAR(10) NOT NULL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admins (
    admin_id VARCHAR(36) NOT NULL PRIMARY KEY,
    phone_code VARCHAR(10) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_admins_phone UNIQUE (phone_code, phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_roles (
    admin_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (admin_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add foreign key constraints only if they don't exist
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_roles'
    AND CONSTRAINT_NAME = 'fk_admin_roles_admin');

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE admin_roles ADD CONSTRAINT fk_admin_roles_admin FOREIGN KEY (admin_id) REFERENCES admins(admin_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_admin_roles_admin already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_roles'
    AND CONSTRAINT_NAME = 'fk_admin_roles_role');

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE admin_roles ADD CONSTRAINT fk_admin_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_admin_roles_role already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create indexes for users table
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_email');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_users_email ON users (email)', 'SELECT ''Index idx_users_email already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_phone');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_users_phone ON users (phone_code, phone_number)', 'SELECT ''Index idx_users_phone already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_first_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_users_first_name ON users (first_name)', 'SELECT ''Index idx_users_first_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_last_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_users_last_name ON users (last_name)', 'SELECT ''Index idx_users_last_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create indexes for admins table
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admins'
    AND INDEX_NAME = 'idx_admins_email');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_admins_email ON admins (email)', 'SELECT ''Index idx_admins_email already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admins'
    AND INDEX_NAME = 'idx_admins_phone');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_admins_phone ON admins (phone_code, phone_number)', 'SELECT ''Index idx_admins_phone already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admins'
    AND INDEX_NAME = 'idx_admins_first_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_admins_first_name ON admins (first_name)', 'SELECT ''Index idx_admins_first_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admins'
    AND INDEX_NAME = 'idx_admins_last_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_admins_last_name ON admins (last_name)', 'SELECT ''Index idx_admins_last_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create indexes for roles table
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'roles'
    AND INDEX_NAME = 'idx_roles_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_roles_name ON roles (role_name)', 'SELECT ''Index idx_roles_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO roles (role_id, role_name) VALUES
    ('SA', 'Super Admin'),
    ('UA', 'User Admin'),
    ('CM', 'Content Moderator'),
    ('SPA', 'Support Admin'),
    ('FA', 'Finance Admin'),
    ('MA', 'Marketing Admin');
