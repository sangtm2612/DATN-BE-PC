-- Migration: Add pending_deposit status to order_status enum
-- Purpose: Support deposit payment flow for COD orders

-- Add new value to order_status enum
ALTER TYPE order_status ADD VALUE 'pending_deposit' BEFORE 'pending';

-- Add comment
COMMENT ON TYPE order_status IS 'Order status: pending_deposit (chờ thanh toán cọc), pending (chờ xác nhận), confirmed, processing, shipping, delivered, completed, cancelled, refunded';
