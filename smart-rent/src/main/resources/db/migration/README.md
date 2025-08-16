# Database Migration Guide

This directory contains Flyway database migration scripts for the SmartRent application.

## Migration Naming Convention

Flyway migration files must follow this naming pattern:
```
V{version}__{description}.sql
```

Examples:
- `V1__Create_users_table.sql`
- `V2__Add_user_profile_fields.sql`
- `V3__Create_properties_table.sql`
- `V4__Add_indexes_to_properties.sql`

## Migration Rules

1. **Never modify existing migration files** - Once a migration has been applied, it should never be changed
2. **Always use incremental version numbers** - V1, V2, V3, etc.
3. **Use descriptive names** - The description should clearly indicate what the migration does
4. **Test migrations locally first** - Always test on local database before committing
5. **Keep migrations small and focused** - Each migration should do one logical thing
6. **Include rollback instructions** - Add comments explaining how to manually rollback if needed

## Migration Types

### Schema Changes
- Creating/dropping tables
- Adding/removing columns
- Creating/dropping indexes
- Adding/removing constraints

### Data Changes
- Inserting reference data
- Updating existing data
- Data cleanup operations

## Best Practices

1. **Always add timestamps** to new tables:
   ```sql
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   ```

2. **Use appropriate data types**:
   - VARCHAR(255) for IDs and short strings
   - TEXT for long content
   - BOOLEAN for true/false values
   - TIMESTAMP for date/time values

3. **Add indexes for performance**:
   ```sql
   CREATE INDEX idx_table_column ON table_name (column_name);
   ```

4. **Add constraints for data integrity**:
   ```sql
   ALTER TABLE table_name ADD CONSTRAINT uk_table_column UNIQUE (column_name);
   ```

5. **Include comments** explaining complex migrations:
   ```sql
   -- This migration adds user profile fields to support enhanced user management
   ```

## Running Migrations

Migrations run automatically when the application starts. You can also run them manually:

```bash
# Run all pending migrations
./gradlew flywayMigrate

# Get migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Clean database (WARNING: This will drop all objects)
./gradlew flywayClean
```

## Troubleshooting

### Migration Failed
If a migration fails:
1. Check the error message in the logs
2. Fix the SQL syntax or logic error
3. Manually clean up any partial changes
4. Create a new migration to fix the issue (don't modify the failed one)

### Out of Order Migrations
If you need to add a migration between existing ones:
1. Set `flyway.out-of-order=true` temporarily
2. Add the migration with appropriate version number
3. Reset `flyway.out-of-order=false` after applying

### Baseline Existing Database
For existing databases without migration history:
1. Flyway will automatically baseline at version 0
2. All existing tables will be preserved
3. New migrations will be applied from V1 onwards
