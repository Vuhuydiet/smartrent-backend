# Database Migration Updates Summary

This document summarizes the migration files created to align the database schema with the JPA entity definitions.

## Entity Analysis Results

### Entities Analyzed:
1. **Admin.java** - Admins table with roles relationship
2. **InvalidatedToken.java** - JWT token invalidation tracking
3. **Role.java** - User/Admin roles
4. **User.java** - Application users
5. **VerifyCode.java** - Email/phone verification codes

## Migration Files Created

### V4__Add_missing_user_fields_and_constraints.sql
**Purpose**: Update users table to match User entity

**Changes Made**:
- Added `is_verified` BOOLEAN field (default FALSE)
- Added unique constraint on `id_document` field
- Added unique constraint on `tax_number` field
- Added performance indexes for new fields

**Rationale**: The User entity defines these fields but they were missing from the original migration.

### V5__Fix_admins_table_constraints.sql
**Purpose**: Fix admins table constraints to match Admin entity

**Changes Made**:
- Modified `password` field to be NOT NULL (matches entity requirement)
- Updated existing NULL passwords to empty string before constraint

**Rationale**: The Admin entity marks password as `nullable = false` but the migration allowed NULL values.

### V6__Create_verify_codes_table.sql
**Purpose**: Create missing verify_codes table for VerifyCode entity

**Changes Made**:
- Created complete `verify_codes` table with all required fields
- Added foreign key relationship to users table
- Added performance indexes on user_id and expiration_time
- Included standard created_at/updated_at timestamps

**Rationale**: The VerifyCode entity was completely missing from the database schema.

## Existing Migrations Status

### ✅ Correctly Aligned:
- **V1**: Users, admins, roles, admin_roles tables (mostly correct)
- **V2**: Invalidated_tokens table (structure matches entity)
- **V3**: Rename admin_roles to admins_roles (matches entity relationship)

### ⚠️ Notes:
- All new migrations maintain backward compatibility
- Existing data is preserved with safe defaults
- Foreign key constraints ensure referential integrity
- Indexes added for optimal query performance

## Verification Steps

After running these migrations, verify:

1. **Users table** has `is_verified`, unique constraints on `id_document` and `tax_number`
2. **Admins table** has NOT NULL constraint on `password`
3. **Verify_codes table** exists with proper foreign key to users
4. All indexes are created for performance
5. No data loss occurred during migrations

## Running the Migrations

```bash
# Run all pending migrations
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate
```
