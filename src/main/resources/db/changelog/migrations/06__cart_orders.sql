--liquibase formatted sql

--changeset kinhduanpc:06__cart_orders runOnChange:false splitStatements:true endDelimiter:;
--comment Create cart, orders, payments, shipping and related tables

-- ============================================================
-- PHẦN 6: GIỎ HÀNG (FR-09)
-- ============================================================

-- Giỏ hàng: hỗ trợ cả guest (session) và user đã đăng nhập
CREATE TABLE carts (
                       id         SERIAL    PRIMARY KEY,
                       user_id    INT       REFERENCES users(id) ON DELETE CASCADE,
                       session_id VARCHAR(100),                           -- localStorage token cho guest
                       expires_at TIMESTAMP NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       CONSTRAINT chk_cart_owner CHECK (user_id IS NOT NULL OR session_id IS NOT NULL)
);
CREATE INDEX idx_carts_user    ON carts(user_id);
CREATE INDEX idx_carts_session ON carts(session_id);
CREATE INDEX idx_carts_expiry  ON carts(expires_at);

CREATE TABLE cart_items (
                            id         SERIAL        PRIMARY KEY,
                            cart_id    INT           NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                            product_id INT           NOT NULL REFERENCES products(id),
                            quantity   INT           NOT NULL DEFAULT 1 CHECK (quantity > 0),
                            unit_price NUMERIC(15,2) NOT NULL,               -- Snapshot giá tại thời điểm thêm
                            added_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
                            UNIQUE (cart_id, product_id)
);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

-- ============================================================
-- PHẦN 7: ĐƠN HÀNG (FR-10 ~ FR-17)
-- ============================================================

CREATE TABLE orders (
                        id               SERIAL         PRIMARY KEY,
                        order_code       VARCHAR(30)    NOT NULL UNIQUE,   -- HC-2026-000001
                        user_id          INT            REFERENCES users(id),

    -- Trạng thái
                        status           order_status   NOT NULL DEFAULT 'pending',
                        payment_method   payment_method NOT NULL,
                        payment_status   payment_status NOT NULL DEFAULT 'pending',

    -- Hình thức nhận hàng
                        pickup_store_id  INT,                              -- FK → stores; NULL = giao tận nơi

    -- Địa chỉ giao hàng (snapshot tại lúc đặt)
                        shipping_name    VARCHAR(150)   NOT NULL,
                        shipping_phone   VARCHAR(20)    NOT NULL,
                        shipping_province VARCHAR(100)  NOT NULL,
                        shipping_district VARCHAR(100)  NOT NULL,
                        shipping_ward    VARCHAR(100)   NOT NULL,
                        shipping_address VARCHAR(300)   NOT NULL,

    -- Tiền
                        subtotal         NUMERIC(15,2)  NOT NULL,
                        shipping_fee     NUMERIC(15,2)  NOT NULL DEFAULT 0,
                        discount_amount  NUMERIC(15,2)  NOT NULL DEFAULT 0,
                        total_amount     NUMERIC(15,2)  NOT NULL,
                        refund_amount    NUMERIC(15,2)  NOT NULL DEFAULT 0, -- FR-15: số tiền đã hoàn

    -- Voucher snapshot
                        voucher_id       INT            REFERENCES vouchers(id),
                        voucher_code     VARCHAR(50),

    -- Build PC (nếu đặt từ trang Build PC)
                        build_id         INT            REFERENCES pc_builds(id),

    -- Ghi chú
                        note             TEXT,                             -- Ghi chú của khách
                        staff_note       TEXT,                             -- Ghi chú nội bộ Staff

    -- Lý do hủy
                        cancelled_reason TEXT,

    -- FR-10: Tự động hủy đơn COD sau 48h chưa xác nhận
                        auto_cancel_at   TIMESTAMP,

    -- Timeline
                        confirmed_at     TIMESTAMP,
                        processing_at    TIMESTAMP,
                        shipped_at       TIMESTAMP,
                        delivered_at     TIMESTAMP,
                        completed_at     TIMESTAMP,
                        cancelled_at     TIMESTAMP,
                        refunded_at      TIMESTAMP,

                        created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orders_user    ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_code    ON orders(order_code);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_orders_auto_cancel ON orders(auto_cancel_at)
    WHERE auto_cancel_at IS NOT NULL AND status = 'pending';

-- Chi tiết sản phẩm trong đơn (snapshot dữ liệu tại thời điểm đặt)
CREATE TABLE order_items (
                             id              SERIAL        PRIMARY KEY,
                             order_id        INT           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id      INT           NOT NULL REFERENCES products(id),
    -- Snapshot để giữ dữ liệu dù sản phẩm thay đổi sau này
                             product_name    VARCHAR(300)  NOT NULL,
                             product_sku     VARCHAR(100),
                             product_image   VARCHAR(500),
                             warranty_months INT           NOT NULL DEFAULT 12, -- Snapshot tháng BH tại lúc mua
                             quantity        INT           NOT NULL CHECK (quantity > 0),
                             unit_price      NUMERIC(15,2) NOT NULL,
                             discount_price  NUMERIC(15,2) NOT NULL DEFAULT 0,
                             total_price     NUMERIC(15,2) NOT NULL
);
CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- FK hoàn thiện
ALTER TABLE voucher_usage
    ADD CONSTRAINT fk_voucher_usage_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

-- ============================================================
-- PHẦN 8: THANH TOÁN (FR-10)
-- ============================================================

CREATE TABLE payments (
                          id               SERIAL        PRIMARY KEY,
                          order_id         INT           NOT NULL REFERENCES orders(id),
                          transaction_id   VARCHAR(200)  UNIQUE,
                          gateway          VARCHAR(30),                    -- 'vnpay','momo','zalopay'
                          amount           NUMERIC(15,2) NOT NULL,
                          currency         VARCHAR(10)   NOT NULL DEFAULT 'VND',
                          status           payment_status NOT NULL DEFAULT 'pending',
                          gateway_response JSONB,                          -- Raw callback từ cổng TT
                          paid_at          TIMESTAMP,
                          refunded_at      TIMESTAMP,
                          refund_amount    NUMERIC(15,2),
                          created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_order       ON payments(order_id);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);
CREATE INDEX idx_payments_status      ON payments(status);

-- ============================================================
-- PHẦN 9: GIAO HÀNG (FR-10)
-- ============================================================

CREATE TABLE shipping_methods (
                                  id             SERIAL        PRIMARY KEY,
                                  name           VARCHAR(100)  NOT NULL,
                                  description    VARCHAR(300),
                                  base_fee       NUMERIC(10,2) NOT NULL DEFAULT 0,
                                  free_threshold NUMERIC(15,2),                   -- Miễn phí nếu đơn >= X
                                  estimated_days VARCHAR(50),                     -- "2-3 ngày"
                                  is_active      BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE order_shipping (
                                id              SERIAL        PRIMARY KEY,
                                order_id        INT           NOT NULL UNIQUE REFERENCES orders(id),
                                method_id       INT           REFERENCES shipping_methods(id),
    -- Nhận tại showroom
                                pickup_store_id INT,                            -- FK → stores
                                tracking_code   VARCHAR(100),
                                carrier         VARCHAR(100),                   -- GHN, GHTK, VNPost
                                shipping_fee    NUMERIC(10,2) NOT NULL DEFAULT 0,
                                shipped_at      TIMESTAMP,
                                estimated_at    TIMESTAMP,
                                delivered_at    TIMESTAMP,
                                status          VARCHAR(50)   NOT NULL DEFAULT 'pending',
                                notes           TEXT,
                                created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
                                updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_shipping_tracking ON order_shipping(tracking_code);

-- ============================================================
-- PHẦN 10: TRẢ GÓP (FR-11)
-- ============================================================

CREATE TABLE installment_plans (
                                   id            SERIAL        PRIMARY KEY,
                                   name          VARCHAR(200)  NOT NULL,            -- "Trả góp 0% — 12 tháng"
                                   provider      VARCHAR(100)  NOT NULL,            -- HomeCredit, FE Credit, HSBC, Shinhan
                                   months        INT           NOT NULL,
                                   interest_rate NUMERIC(5,2)  NOT NULL DEFAULT 0,  -- % mỗi tháng
                                   min_order     NUMERIC(15,2) NOT NULL DEFAULT 3000000,
                                   is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE order_installments (
                                    id             SERIAL        PRIMARY KEY,
                                    order_id       INT           NOT NULL REFERENCES orders(id),
                                    plan_id        INT           NOT NULL REFERENCES installment_plans(id),
                                    monthly_amount NUMERIC(15,2) NOT NULL,
                                    total_amount   NUMERIC(15,2) NOT NULL,
                                    application_id VARCHAR(100),                    -- Mã hồ sơ từ đối tác tài chính
    -- FR-11: pending → approved → rejected
                                    status         VARCHAR(20)   NOT NULL DEFAULT 'pending',
                                    approved_at    TIMESTAMP,
                                    rejected_at    TIMESTAMP,
                                    reject_reason  TEXT,
                                    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_installments_order ON order_installments(order_id);

-- ============================================================
-- PHẦN 11: ĐỔI / TRẢ HÀNG (FR-16 — UC-14)
-- ============================================================

CREATE TABLE return_requests (
                                 id             SERIAL        PRIMARY KEY,
                                 order_id       INT           NOT NULL REFERENCES orders(id),
                                 user_id        INT           NOT NULL REFERENCES users(id),
                                 return_code    VARCHAR(30)   NOT NULL UNIQUE,    -- RT-2026-000001
                                 status         return_status NOT NULL DEFAULT 'pending',
    -- Lý do: 'defective','wrong_item','damaged_delivery','not_satisfied'
                                 reason_type    VARCHAR(50)   NOT NULL,
                                 reason_detail  TEXT,
    -- Kết quả xử lý
                                 resolution     VARCHAR(30),                     -- 'exchange','refund'
                                 refund_amount  NUMERIC(15,2),
                                 staff_note     TEXT,
                                 reviewed_by    INT           REFERENCES users(id),
                                 reviewed_at    TIMESTAMP,
                                 completed_at   TIMESTAMP,
                                 created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
                                 updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
    -- NOTE: Điều kiện 15 ngày đổi/trả được kiểm tra ở tầng application (OrderService)
);
CREATE INDEX idx_return_requests_order ON return_requests(order_id);
CREATE INDEX idx_return_requests_user  ON return_requests(user_id);
CREATE INDEX idx_return_requests_code  ON return_requests(return_code);

-- Sản phẩm trong yêu cầu đổi/trả
CREATE TABLE return_request_items (
                                      id                SERIAL PRIMARY KEY,
                                      return_request_id INT    NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
                                      order_item_id     INT    NOT NULL REFERENCES order_items(id),
                                      quantity          INT    NOT NULL CHECK (quantity > 0)
);

-- Media đính kèm (ảnh/video chứng minh lỗi — FR-16: tối đa 5 file)
CREATE TABLE return_media (
                              id                SERIAL PRIMARY KEY,
                              return_request_id INT          NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
                              media_url         VARCHAR(500) NOT NULL,
                              media_type        VARCHAR(10)  NOT NULL DEFAULT 'image', -- 'image','video'
                              sort_order        INT          NOT NULL DEFAULT 0
);

-- ============================================================
-- PHẦN 12: HÓA ĐƠN ĐIỆN TỬ (FR-17 — UC-15)
-- ============================================================

CREATE TABLE invoice_requests (
                                  id            SERIAL       PRIMARY KEY,
                                  order_id      INT          NOT NULL UNIQUE REFERENCES orders(id),
                                  user_id       INT          NOT NULL REFERENCES users(id),
                                  invoice_type  invoice_type NOT NULL DEFAULT 'personal',
    -- Thông tin cá nhân / doanh nghiệp
                                  buyer_name    VARCHAR(200) NOT NULL,
                                  buyer_address VARCHAR(400),
                                  buyer_email   VARCHAR(200) NOT NULL,
    -- Chỉ cần khi invoice_type = 'company'
                                  company_name  VARCHAR(200),
                                  tax_code      VARCHAR(20),
    -- File PDF đã tạo
                                  pdf_url       VARCHAR(500),
                                  issued_at     TIMESTAMP,
                                  created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_invoice_requests_order ON invoice_requests(order_id);
CREATE INDEX idx_invoice_requests_user  ON invoice_requests(user_id);
