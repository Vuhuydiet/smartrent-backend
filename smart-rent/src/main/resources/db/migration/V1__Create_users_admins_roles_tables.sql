CREATE TABLE users (
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

CREATE TABLE roles (
    role_id VARCHAR(10) NOT NULL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admins (
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

CREATE TABLE admin_roles (
    admin_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (admin_id, role_id),

    CONSTRAINT fk_admin_roles_admin FOREIGN KEY (admin_id) REFERENCES admins(admin_id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone ON users (phone_code, phone_number);
CREATE INDEX idx_users_first_name ON users (first_name);
CREATE INDEX idx_users_last_name ON users (last_name);

CREATE INDEX idx_admins_email ON admins (email);
CREATE INDEX idx_admins_phone ON admins (phone_code, phone_number);
CREATE INDEX idx_admins_first_name ON admins (first_name);
CREATE INDEX idx_admins_last_name ON admins (last_name);

CREATE INDEX idx_roles_name ON roles (role_name);

INSERT INTO roles (role_id, role_name) VALUES
    ('SA', 'Super Admin'),
    ('UA', 'User Admin'),
    ('CM', 'Content Moderator'),
    ('SPA', 'Support Admin'),
    ('FA', 'Finance Admin'),
    ('MA', 'Marketing Admin');
