-- Drop all tables created by V13 migration (in reverse order of dependencies)
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS push_history;
DROP TABLE IF EXISTS push_schedule;
DROP TABLE IF EXISTS user_membership_benefits;
DROP TABLE IF EXISTS membership_package_benefits;
DROP TABLE IF EXISTS user_memberships;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS membership_packages;

SET FOREIGN_KEY_CHECKS = 1;
