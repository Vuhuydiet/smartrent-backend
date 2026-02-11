-- Add is_verified field
ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE;

-- Add unique constraints for id_document and tax_number
ALTER TABLE users ADD CONSTRAINT uk_users_id_document UNIQUE (id_document);
ALTER TABLE users ADD CONSTRAINT uk_users_tax_number UNIQUE (tax_number);

-- Add indexes for better performance
CREATE INDEX idx_users_id_document ON users (id_document);
CREATE INDEX idx_users_tax_number ON users (tax_number);
CREATE INDEX idx_users_is_verified ON users (is_verified);
