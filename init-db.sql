-- ============================================================
-- Banking Payment System — Database Initialization Script
-- This script runs automatically when MySQL container starts
-- ============================================================

-- Account Service Database
CREATE DATABASE IF NOT EXISTS account_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Payment Service Database
CREATE DATABASE IF NOT EXISTS payment_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Notification Service Database
CREATE DATABASE IF NOT EXISTS notification_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Fraud Detection Service Database
CREATE DATABASE IF NOT EXISTS fraud_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Audit Service Database
CREATE DATABASE IF NOT EXISTS audit_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Analytics Service Database
CREATE DATABASE IF NOT EXISTS analytics_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to root user for all databases
GRANT ALL PRIVILEGES ON account_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON fraud_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON audit_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON analytics_db.* TO 'root'@'%';

FLUSH PRIVILEGES;

-- ============================================================
-- NOTE: JPA (hibernate.ddl-auto=update) will create
-- the individual tables automatically when each Spring Boot
-- service starts. We only pre-create the databases here.
-- ============================================================

-- Sample accounts for testing (Phase 1)
USE account_db;

-- Tables are created by JPA. We'll insert test data via API or
-- use the /api/accounts POST endpoint to create sample accounts.
-- See: docs/test-data.sql for sample insert scripts.
