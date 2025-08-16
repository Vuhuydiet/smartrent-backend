# Database Migration Setup - SmartRent Backend

This document describes the database migration setup for the SmartRent backend application.

## Overview

The SmartRent application uses **Flyway** for database schema versioning and migration management. Flyway is integrated with Spring Boot and provides automatic migration execution on application startup.

## Technology Stack

- **Database**: MySQL (production), H2 (testing)
- **Migration Tool**: Flyway 10.21.0
- **Framework**: Spring Boot 3.5.4
- **Build Tool**: Gradle

## Configuration

### Application Properties

Flyway is configured in both `application.yml` and `application-local.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
```

### Gradle Configuration

The Flyway Gradle plugin is configured in `build.gradle` for manual migration management:

```gradle
plugins {
    id 'org.flywaydb.flyway' version '10.21.0'
}

flyway {
    url = 'jdbc:mysql://localhost:3306/smartrent'
    user = 'root'
    password = 'root'
    locations = ['classpath:db/migration']
    baselineOnMigrate = true
    baselineVersion = '0'
    validateOnMigrate = true
}
```

## Migration Files

Migration files are located in `src/main/resources/db/migration/` and follow Flyway naming conventions:

### Current Migrations

1. **V1__Create_users_table.sql** - Creates the initial users table with basic authentication fields (id, username, password)
2. **V2__Add_user_profile_fields.sql** - Adds profile fields (email, names, phone, etc.) - *Future enhancement*
3. **V3__Insert_default_admin_user.sql** - Creates default admin user for initial access
4. **V4__Create_properties_table.sql** - Creates properties table for rental property management

### Migration Structure

Each migration includes:
- Descriptive comments
- SQL DDL/DML statements
- Appropriate indexes for performance
- Constraints for data integrity
- Timestamps for audit trails

## Usage

### Automatic Migration (Recommended)

Migrations run automatically when the Spring Boot application starts. This is the default behavior in all environments.

### Manual Migration Commands

Use the provided helper script for manual migration management:

```bash
# Create a new migration
./scripts/migration-helper.sh create "Add tenant table"

# Run pending migrations
./scripts/migration-helper.sh migrate

# Check migration status
./scripts/migration-helper.sh info

# Validate migrations
./scripts/migration-helper.sh validate
```

### Gradle Commands

Direct Gradle commands for migration management:

```bash
# Run migrations
./gradlew flywayMigrate

# Get migration info
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Clean database (WARNING: Destructive)
./gradlew flywayClean
```

## Development Workflow

### Creating New Migrations

1. **Use the helper script**:
   ```bash
   ./scripts/migration-helper.sh create "Your migration description"
   ```

2. **Edit the generated file** with your SQL statements

3. **Test locally** before committing:
   ```bash
   ./scripts/migration-helper.sh migrate
   ```

4. **Commit the migration file** to version control

### Best Practices

- **Never modify existing migrations** once they're committed
- **Test migrations on a copy of production data** before deploying
- **Keep migrations small and focused** on single logical changes
- **Include rollback instructions** in comments when applicable
- **Use descriptive names** that clearly indicate the migration purpose

## Database Schema

### Current Tables

#### users
- Primary table for user authentication
- Contains basic fields: id (VARCHAR), username (VARCHAR), password (VARCHAR)
- Has unique constraint on username for login uniqueness

#### properties
- Manages rental properties
- Links to users table via owner_id foreign key
- Supports different property types and amenities

## Environment Configuration

### Local Development
- Database: `jdbc:mysql://localhost:3306/smartrent`
- Credentials: root/root
- Auto-migration: Enabled

### Production
- Database URL: Set via `IDENTITY_SERVICE_DB_URL` environment variable
- Credentials: Set via `DB_USERNAME` and `DB_PASSWORD` environment variables
- Auto-migration: Enabled with validation

## Troubleshooting

### Common Issues

1. **Migration Checksum Mismatch**
   - Cause: Modified existing migration file
   - Solution: Revert changes or create repair migration

2. **Failed Migration**
   - Check application logs for SQL errors
   - Manually clean up partial changes
   - Create new migration to fix issues

3. **Out of Order Migrations**
   - Temporarily enable `out-of-order: true`
   - Apply the migration
   - Disable out-of-order again

### Getting Help

- Check the migration README: `src/main/resources/db/migration/README.md`
- Use the helper script: `./scripts/migration-helper.sh help`
- Review Flyway documentation: https://flywaydb.org/documentation/

## Security Considerations

- Default admin user password should be changed immediately in production
- Database credentials should be stored securely (environment variables)
- Migration files should not contain sensitive data in plain text
- Use proper BCrypt hashing for password fields
