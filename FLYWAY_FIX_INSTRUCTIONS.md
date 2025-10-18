# Fix Flyway Migration Error - Manual Steps

## Problem
Migration V13 failed during execution and is now marked as failed in the Flyway schema history table. The application cannot start until this is resolved.

## Solution

You need to manually connect to your MySQL database and remove the failed migration record.

### Option 1: Using MySQL Workbench or any MySQL GUI tool

1. Open MySQL Workbench (or your preferred MySQL GUI client)
2. Connect to your database:
   - Host: localhost
   - Port: 3306
   - Database: smartrent
   - Username: root
   - Password: vuhuydiet

3. Run this SQL query:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '13' AND success = 0;
   ```

4. Verify the record was deleted:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

### Option 2: Using MySQL Command Line

1. Open Command Prompt or PowerShell as Administrator
2. Navigate to your MySQL installation bin folder (e.g., `C:\Program Files\MySQL\MySQL Server 8.0\bin`)
3. Run:
   ```
   mysql -u root -pvuhuydiet smartrent
   ```
4. Execute:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '13' AND success = 0;
   ```
5. Type `exit` to quit MySQL

### Option 3: Add MySQL to PATH and run from any terminal

1. Add MySQL bin folder to your system PATH
2. Open PowerShell and run:
   ```powershell
   mysql -u root -pvuhuydiet smartrent -e "DELETE FROM flyway_schema_history WHERE version = '13' AND success = 0;"
   ```

## What Was Fixed in the Migration File

The migration file `V13__Create_membership_and_transaction_system.sql` has been corrected with the following changes:

1. **Added `user_id` column** to `push_schedule` table (was missing)
2. **Added foreign key constraint** for `user_id` in `push_schedule` table
3. **Fixed primary key column name** in `push_history` table from `push_history_id` to `push_id`
4. **Fixed foreign key reference** in `push_history` table to correctly reference `user_membership_benefits(user_benefit_id)`
5. **Added missing `fk_ph_schedule` constraint** for `push_history` to `push_schedule` relationship

## After Fixing the Database

Once you've deleted the failed migration record from the database, run:

```powershell
cd D:\Personal\Dev\smartrent-backend\smart-rent
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

The application should now start successfully and Flyway will re-run the corrected V13 migration.

## Summary of Files Changed

1. `src/main/resources/db/migration/V13__Create_membership_and_transaction_system.sql` - Fixed to match entity definitions
2. `src/main/resources/application.yml` - Temporarily disabled Flyway validation (you can re-enable it after successful migration)
