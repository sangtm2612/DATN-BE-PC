-- liquibase formatted sql

-- changeset kiro:guest-checkout-support
-- comment: Thêm hỗ trợ guest checkout (đặt hàng không cần đăng nhập)

-- =====================================================
-- Migration: Guest Checkout Support
-- Description: Thêm hỗ trợ guest checkout (đặt hàng không cần đăng nhập)
-- Date: 2026-09-16
-- Database: PostgreSQL
-- =====================================================

-- 1. Thêm session_id column vào orders table
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name='orders' AND column_name='session_id'
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS session_id VARCHAR(100);

-- 4. Thêm session_id vào cart_items (nếu chưa có)
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name='cart_items' AND column_name='session_id'
ALTER TABLE cart_items 
ADD COLUMN IF NOT EXISTS session_id VARCHAR(100);


-- =====================================================
-- Rollback (nếu cần):
-- 
-- ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_user_or_session;
-- DROP INDEX IF EXISTS idx_orders_session_id;
-- DROP INDEX IF EXISTS idx_orders_user_id_status;
-- DROP INDEX IF EXISTS idx_orders_session_id_status;
-- DROP INDEX IF EXISTS idx_cart_session_id;
-- ALTER TABLE orders DROP COLUMN IF EXISTS session_id;
-- ALTER TABLE orders ALTER COLUMN user_id SET NOT NULL;
-- ALTER TABLE cart_items DROP COLUMN IF EXISTS session_id;
-- =====================================================
