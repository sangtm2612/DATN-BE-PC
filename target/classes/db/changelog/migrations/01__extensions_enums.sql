--liquibase formatted sql

--changeset kinhduanpc:01__extensions_enums runOnChange:false splitStatements:true endDelimiter:;
--comment Create extensions and enum types

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "unaccent";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";   -- trigram index cho LIKE search nhanh

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE user_role       AS ENUM ('customer', 'staff', 'technician', 'admin');
CREATE TYPE user_status     AS ENUM ('active', 'inactive', 'banned');
CREATE TYPE discount_type   AS ENUM ('percent', 'fixed_amount', 'free_shipping');
CREATE TYPE order_status    AS ENUM (
    'pending',      -- Chờ xác nhận
    'confirmed',    -- Đã xác nhận
    'processing',   -- Đang đóng gói
    'shipping',     -- Đang giao hàng
    'delivered',    -- Đã giao
    'completed',    -- Hoàn thành (sau 7 ngày giao)
    'cancelled',    -- Đã hủy
    'refunded'      -- Đã hoàn tiền
);
CREATE TYPE payment_method  AS ENUM (
    'cod', 'bank_transfer', 'vnpay', 'momo', 'zalopay', 'installment'
);
CREATE TYPE payment_status  AS ENUM ('pending', 'paid', 'failed', 'refunded');
CREATE TYPE warranty_status AS ENUM ('active', 'expired', 'voided', 'in_service');
CREATE TYPE service_status  AS ENUM (
    'received', 'diagnosing', 'repairing', 'waiting_part', 'done', 'returned'
);
CREATE TYPE return_status   AS ENUM (
    'pending',      -- Khách vừa tạo yêu cầu
    'reviewing',    -- Staff đang xem xét
    'approved',     -- Chấp nhận
    'rejected',     -- Từ chối
    'completed'     -- Đã hoàn tất (đổi hàng/hoàn tiền xong)
);
CREATE TYPE notification_type AS ENUM (
    'order_update', 'promotion', 'warranty_expiry',
    'service_update', 'return_update', 'system'
);
CREATE TYPE promotion_type  AS ENUM (
    'general',          -- Khuyến mãi thông thường
    'build_pc',         -- Chính sách giảm giá Build PC
    'student',          -- Ưu đãi học sinh sinh viên
    'brand_deal',       -- Khuyến mãi theo thương hiệu
    'give_away',        -- Give Away / Quay số
    'flash_sale'        -- Flash Sale có đồng hồ đếm ngược
);
CREATE TYPE invoice_type    AS ENUM ('personal', 'company');
