-- Migration: Add deposit tracking fields to orders table
-- Purpose: Track deposit payment for COD orders

-- Add deposit_amount column
ALTER TABLE orders
    ADD COLUMN deposit_amount NUMERIC(15, 2) DEFAULT 0 NOT NULL;

-- Add deposit_paid flag
ALTER TABLE orders
    ADD COLUMN deposit_paid BOOLEAN DEFAULT FALSE NOT NULL;

-- Add remaining_amount column (calculated field for display)
ALTER TABLE orders
    ADD COLUMN remaining_amount NUMERIC(15, 2);

-- Update existing orders: remaining_amount = total_amount - deposit_amount
UPDATE orders
    SET remaining_amount = total_amount - deposit_amount;

-- Add comments for documentation
COMMENT ON COLUMN orders.deposit_amount IS 'Số tiền cọc (thường là 100k cho COD)';
COMMENT ON COLUMN orders.deposit_paid IS 'Đã thanh toán cọc hay chưa';
COMMENT ON COLUMN orders.remaining_amount IS 'Số tiền còn phải thanh toán = total_amount - deposit_amount';
