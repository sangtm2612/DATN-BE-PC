-- Migration: Add guest_email column to orders
-- Purpose: Store guest email at order creation time so deposit-confirmed email can be sent via payment callback

ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);

COMMENT ON COLUMN orders.guest_email IS 'Email của khách hàng chưa đăng nhập, dùng để gửi email xác nhận cọc';
