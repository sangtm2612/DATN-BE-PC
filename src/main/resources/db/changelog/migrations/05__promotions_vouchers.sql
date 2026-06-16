--liquibase formatted sql

--changeset kinhduanpc:05__promotions_vouchers runOnChange:false splitStatements:true endDelimiter:;
--comment Create promotions and vouchers tables

-- ============================================================
-- PHẦN 5: KHUYẾN MÃI & VOUCHER (FR-22, FR-23)
-- ============================================================

-- Chương trình khuyến mãi (tự động áp dụng theo sản phẩm/danh mục/thương hiệu)
CREATE TABLE promotions (
                            id              SERIAL          PRIMARY KEY,
                            promotion_type  promotion_type  NOT NULL DEFAULT 'general',
                            name            VARCHAR(200)    NOT NULL,
                            description     TEXT,
                            discount_type   discount_type   NOT NULL,
                            discount_value  NUMERIC(10,2)   NOT NULL,
                            min_order_value NUMERIC(15,2)   NOT NULL DEFAULT 0,
                            max_discount    NUMERIC(15,2),                  -- Giới hạn giảm tối đa (cho loại %)
    -- Flash sale: hiện đồng hồ đếm ngược
                            start_date      TIMESTAMP       NOT NULL,
                            end_date        TIMESTAMP       NOT NULL,
                            is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Build PC policy fields (FR-08)
                            buildpc_min_cpu_discount_pct    NUMERIC(5,2),   -- VD: 30.00
                            buildpc_max_cpu_discount_pct    NUMERIC(5,2),   -- VD: 50.00
                            buildpc_cash_bonus              NUMERIC(15,2),  -- Tiền mặt tặng thêm
                            buildpc_max_cash_bonus          NUMERIC(15,2),  -- Tối đa 30 triệu
                            created_by      INT             REFERENCES users(id),
                            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
                            CONSTRAINT chk_promotion_dates CHECK (end_date > start_date)
);
CREATE INDEX idx_promotions_active ON promotions(is_active, start_date, end_date);
CREATE INDEX idx_promotions_type   ON promotions(promotion_type);

-- Phạm vi áp dụng KM: sản phẩm cụ thể
CREATE TABLE promotion_products (
                                    promotion_id INT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
                                    product_id   INT NOT NULL REFERENCES products(id)   ON DELETE CASCADE,
                                    PRIMARY KEY (promotion_id, product_id)
);

-- Phạm vi áp dụng KM: danh mục
CREATE TABLE promotion_categories (
                                      promotion_id INT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
                                      category_id  INT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
                                      PRIMARY KEY (promotion_id, category_id)
);

-- Phạm vi áp dụng KM: thương hiệu (FR-22: KM theo brand ASUS, MSI...)
CREATE TABLE promotion_brands (
                                  promotion_id INT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
                                  brand_id     INT NOT NULL REFERENCES brands(id)     ON DELETE CASCADE,
                                  PRIMARY KEY (promotion_id, brand_id)
);

-- Voucher / mã giảm giá nhập tay (FR-23)
CREATE TABLE vouchers (
                          id              SERIAL        PRIMARY KEY,
                          code            VARCHAR(50)   NOT NULL UNIQUE,
                          name            VARCHAR(200),
                          discount_type   discount_type NOT NULL,
                          discount_value  NUMERIC(10,2) NOT NULL,
                          min_order_value NUMERIC(15,2) NOT NULL DEFAULT 0,
                          max_discount    NUMERIC(15,2),
                          usage_limit     INT,                            -- NULL = không giới hạn tổng
                          usage_per_user  INT           NOT NULL DEFAULT 1,
                          used_count      INT           NOT NULL DEFAULT 0,
                          start_date      TIMESTAMP     NOT NULL,
                          end_date        TIMESTAMP     NOT NULL,
                          is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
                          created_by      INT           REFERENCES users(id),
                          created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
                          CONSTRAINT chk_voucher_dates CHECK (end_date > start_date)
);
CREATE INDEX idx_vouchers_code   ON vouchers(code);
CREATE INDEX idx_vouchers_active ON vouchers(is_active, start_date, end_date);

-- Lịch sử sử dụng voucher
CREATE TABLE voucher_usage (
                               id          SERIAL    PRIMARY KEY,
                               voucher_id  INT       NOT NULL REFERENCES vouchers(id),
                               user_id     INT       NOT NULL REFERENCES users(id),
                               order_id    INT,                                -- FK thêm sau khi có bảng orders
                               used_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_voucher_usage_voucher ON voucher_usage(voucher_id);
CREATE INDEX idx_voucher_usage_user    ON voucher_usage(user_id);
