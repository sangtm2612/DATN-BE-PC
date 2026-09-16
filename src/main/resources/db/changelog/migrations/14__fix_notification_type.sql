-- Migration: Fix notification type column from enum to varchar
-- Purpose: Resolve type mismatch between Hibernate @Enumerated(EnumType.STRING) and PostgreSQL enum

-- Step 1: Alter the notifications table to change type column from notification_type enum to VARCHAR
ALTER TABLE notifications 
    ALTER COLUMN type TYPE VARCHAR(50) USING type::text;

-- Note: We keep the notification_type enum in database for reference,
-- but the table now uses VARCHAR for compatibility with JPA @Enumerated(EnumType.STRING)
