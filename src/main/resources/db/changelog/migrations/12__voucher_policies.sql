--liquibase formatted sql

--changeset kinhduanpc:12a__voucher_type_and_user_vouchers runOnChange:false splitStatements:true endDelimiter:;
--comment Add voucher_type column to vouchers and create user_vouchers table

-- ============================================================
-- BỔ SUNG CỘT voucher_type CHO BẢNG vouchers
-- PUBLIC: ai biết mã đều dùng được
-- PERSONAL: chỉ user được phân phối mới dùng được
-- ============================================================

ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS voucher_type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- ============================================================
-- BẢNG user_vouchers: LƯU VOUCHER ĐƯỢC PHÂN PHỐI CHO USER
-- ============================================================

CREATE TABLE IF NOT EXISTS user_vouchers (
    id              SERIAL          PRIMARY KEY,
    user_id         INT             NOT NULL REFERENCES users(id),
    voucher_id      INT             NOT NULL REFERENCES vouchers(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',   -- AVAILABLE, USED, EXPIRED
    assigned_by     INT,                                             -- Admin ID người tặng (NULL = tự động)
    assigned_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    used_at         TIMESTAMP,
    order_id        INT,                                             -- Đơn hàng đã sử dụng
    expires_at      TIMESTAMP,                                       -- HSD riêng cho user
    UNIQUE(user_id, voucher_id)
);

CREATE INDEX IF NOT EXISTS idx_user_vouchers_user ON user_vouchers(user_id);
CREATE INDEX IF NOT EXISTS idx_user_vouchers_status ON user_vouchers(user_id, status);

--changeset kinhduanpc:12b__voucher_policies runOnChange:false splitStatements:true endDelimiter:;
--comment Create voucher_policies table for automatic voucher distribution

-- ============================================================
-- CHÍNH SÁCH PHÂN PHỐI VOUCHER TỰ ĐỘNG
-- Ai nhận? Điều kiện gì? Nhận như thế nào?
-- ============================================================

CREATE TABLE IF NOT EXISTS voucher_policies (
    id                      SERIAL          PRIMARY KEY,
    name                    VARCHAR(200)    NOT NULL,
    description             TEXT,

    -- Sự kiện kích hoạt: WELCOME, FIRST_ORDER, BIRTHDAY, SPENDING_MILESTONE, ORDER_COUNT, REVIEW_REWARD
    trigger_type            VARCHAR(30)     NOT NULL,

    -- Voucher mẫu sẽ được phát (phải là loại PERSONAL)
    voucher_id              INT             NOT NULL REFERENCES vouchers(id),

    -- ===== Điều kiện đủ =====
    min_total_spent         NUMERIC(15,2),              -- Tổng chi tiêu tối thiểu
    min_completed_orders    INT,                         -- Số đơn hoàn thành tối thiểu
    min_account_age_days    INT,                         -- Tuổi tài khoản tối thiểu (ngày)
    spending_milestone      NUMERIC(15,2),              -- Mốc chi tiêu (cho SPENDING_MILESTONE)
    order_count_milestone   INT,                         -- Mốc số đơn (cho ORDER_COUNT)

    -- ===== Giới hạn phân phối =====
    max_distributions       INT,                         -- Tổng số lượt phát tối đa (NULL = không giới hạn)
    distributed_count       INT             NOT NULL DEFAULT 0,  -- Số lượt đã phát
    one_per_user            BOOLEAN         NOT NULL DEFAULT TRUE, -- Mỗi user chỉ nhận 1 lần
    custom_expires_days     INT,                         -- Số ngày hiệu lực từ khi nhận (NULL = theo voucher gốc)

    -- ===== Thời gian hiệu lực chính sách =====
    start_date              TIMESTAMP       NOT NULL,
    end_date                TIMESTAMP       NOT NULL,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voucher_policies_trigger ON voucher_policies(trigger_type, is_active);
CREATE INDEX IF NOT EXISTS idx_voucher_policies_active ON voucher_policies(is_active, start_date, end_date);
