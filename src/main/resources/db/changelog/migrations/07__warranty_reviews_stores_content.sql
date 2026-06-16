--liquibase formatted sql

--changeset kinhduanpc:07__warranty_reviews_stores_content runOnChange:false splitStatements:true endDelimiter:;
--comment Create warranty, reviews, stores, blog, banners and other content tables

-- ============================================================
-- PHẦN 13: BẢO HÀNH & SỬA CHỮA (FR-18 ~ FR-20)
-- ============================================================

CREATE TABLE warranties (
                            id              SERIAL         PRIMARY KEY,
                            order_item_id   INT            REFERENCES order_items(id),
                            product_id      INT            NOT NULL REFERENCES products(id),
                            user_id         INT            REFERENCES users(id),
                            serial_number   VARCHAR(200),
                            purchase_date   DATE           NOT NULL,
                            warranty_months INT            NOT NULL DEFAULT 12,
                            warranty_expires_at DATE       NOT NULL,
                            status          warranty_status NOT NULL DEFAULT 'active',
                            notes           TEXT,
                            created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
                            updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_warranties_user    ON warranties(user_id);
CREATE INDEX idx_warranties_serial  ON warranties(serial_number);
CREATE INDEX idx_warranties_product ON warranties(product_id);
CREATE INDEX idx_warranties_expiry  ON warranties(warranty_expires_at);

-- Yêu cầu sửa chữa / bảo hành (FR-20)
CREATE TABLE service_requests (
                                  id                      SERIAL         PRIMARY KEY,
                                  warranty_id             INT            REFERENCES warranties(id),
                                  user_id                 INT            REFERENCES users(id),
                                  store_id                INT,                            -- FK → stores
                                  service_code            VARCHAR(30)    UNIQUE,          -- SV-2026-000001
                                  product_name            VARCHAR(300)   NOT NULL,
                                  serial_number           VARCHAR(200),
                                  issue_desc              TEXT           NOT NULL,
                                  status                  service_status NOT NULL DEFAULT 'received',
                                  diagnosis               TEXT,
    -- FR-20: Báo giá + khách duyệt trước khi sửa (ngoài bảo hành)
                                  repair_cost             NUMERIC(15,2)  NOT NULL DEFAULT 0,
                                  customer_approved_repair BOOLEAN,                       -- NULL=chờ, TRUE=đồng ý, FALSE=từ chối
                                  approved_at             TIMESTAMP,
    -- FR-20: Đặt lịch hẹn mang máy đến
                                  appointment_date        TIMESTAMP,
                                  received_at             TIMESTAMP      NOT NULL DEFAULT NOW(),
                                  completed_at            TIMESTAMP,
                                  returned_at             TIMESTAMP,
                                  technician_id           INT            REFERENCES users(id),
                                  created_at              TIMESTAMP      NOT NULL DEFAULT NOW(),
                                  updated_at              TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_service_requests_user  ON service_requests(user_id);
CREATE INDEX idx_service_requests_code  ON service_requests(service_code);
CREATE INDEX idx_service_requests_store ON service_requests(store_id);

-- Media đính kèm phiếu sửa chữa (ảnh/video lỗi — FR-20: tối đa 5 file)
CREATE TABLE service_media (
                               id                 SERIAL PRIMARY KEY,
                               service_request_id INT          NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
                               media_url          VARCHAR(500) NOT NULL,
                               media_type         VARCHAR(10)  NOT NULL DEFAULT 'image',
                               uploaded_by        VARCHAR(20)  NOT NULL DEFAULT 'customer', -- 'customer','technician'
                               sort_order         INT          NOT NULL DEFAULT 0
);

-- ============================================================
-- PHẦN 14: ĐÁNH GIÁ SẢN PHẨM (FR-21)
-- ============================================================

CREATE TABLE reviews (
                         id            SERIAL   PRIMARY KEY,
                         product_id    INT      NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                         user_id       INT      NOT NULL REFERENCES users(id),
                         order_item_id INT      REFERENCES order_items(id),     -- Verified purchase
                         rating        SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         title         VARCHAR(200),
                         content       TEXT     CHECK (content IS NULL OR LENGTH(content) <= 1000),
                         is_verified   BOOLEAN  NOT NULL DEFAULT FALSE,          -- Đã mua hàng
                         is_approved   BOOLEAN  NOT NULL DEFAULT TRUE,
                         helpful_count INT      NOT NULL DEFAULT 0,
                         created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                         UNIQUE (user_id, order_item_id)
);
CREATE INDEX idx_reviews_product  ON reviews(product_id);
CREATE INDEX idx_reviews_user     ON reviews(user_id);
CREATE INDEX idx_reviews_approved ON reviews(product_id, is_approved);

-- Ảnh trong review (tối đa 5 ảnh — FR-21)
CREATE TABLE review_images (
                               id         SERIAL PRIMARY KEY,
                               review_id  INT          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                               image_url  VARCHAR(500) NOT NULL,
                               sort_order INT          NOT NULL DEFAULT 0
);

-- Đánh dấu review hữu ích (FR-21: "Hữu ích")
CREATE TABLE review_helpful (
                                user_id    INT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                review_id  INT       NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                PRIMARY KEY (user_id, review_id)
);

-- ============================================================
-- PHẦN 15: CỬA HÀNG / SHOWROOM (FR-29)
-- ============================================================

CREATE TABLE stores (
                        id             SERIAL        PRIMARY KEY,
                        name           VARCHAR(200)  NOT NULL,
                        slug           VARCHAR(250)  UNIQUE,
                        address        VARCHAR(400)  NOT NULL,
                        province       VARCHAR(100)  NOT NULL,
                        district       VARCHAR(100),
                        phone          VARCHAR(20),
                        warranty_phone VARCHAR(20),                    -- Số máy lẻ bảo hành (từ thực tế hacom.vn)
                        email          VARCHAR(150),
                        open_hours     VARCHAR(200),                   -- "8:00 - 21:00 hàng ngày"
                        break_hours    VARCHAR(100),                   -- "Nghỉ trưa 12:00-13:30"
                        store_type     VARCHAR(30)   NOT NULL DEFAULT 'showroom', -- 'showroom','hub','online'
                        map_url        VARCHAR(500),                   -- Google Maps link
                        latitude       NUMERIC(10,7),
                        longitude      NUMERIC(10,7),
                        is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
                        created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stores_province ON stores(province);
CREATE INDEX idx_stores_active   ON stores(is_active);

-- Gallery ảnh showroom (FR-29: xem ảnh thực tế)
CREATE TABLE store_images (
                              id         SERIAL PRIMARY KEY,
                              store_id   INT          NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
                              image_url  VARCHAR(500) NOT NULL,
                              caption    VARCHAR(200),
                              sort_order INT          NOT NULL DEFAULT 0
);

-- FK hoàn thiện cho service_requests và order_shipping
ALTER TABLE service_requests
    ADD CONSTRAINT fk_service_store
        FOREIGN KEY (store_id) REFERENCES stores(id);

ALTER TABLE order_shipping
    ADD CONSTRAINT fk_order_shipping_pickup_store
        FOREIGN KEY (pickup_store_id) REFERENCES stores(id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_pickup_store
        FOREIGN KEY (pickup_store_id) REFERENCES stores(id);

ALTER TABLE pc_builds
    ADD CONSTRAINT fk_pc_builds_store
        FOREIGN KEY (store_id) REFERENCES stores(id);

-- Tồn kho theo showroom (FR-30: quản lý tồn kho từng chi nhánh)
CREATE TABLE product_stock_by_store (
                                        product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                        store_id   INT NOT NULL REFERENCES stores(id)   ON DELETE CASCADE,
                                        stock_qty  INT NOT NULL DEFAULT 0,
                                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                        PRIMARY KEY (product_id, store_id)
);
CREATE INDEX idx_stock_by_store_store ON product_stock_by_store(store_id);

-- ============================================================
-- PHẦN 16: TIN TỨC / BLOG (FR-27)
-- ============================================================

CREATE TABLE blog_categories (
                                 id         SERIAL PRIMARY KEY,
                                 name       VARCHAR(150) NOT NULL,
                                 slug       VARCHAR(200) NOT NULL UNIQUE,
                                 sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE blog_posts (
                            id           SERIAL    PRIMARY KEY,
                            category_id  INT       REFERENCES blog_categories(id),
                            author_id    INT       REFERENCES users(id),
                            title        VARCHAR(300) NOT NULL,
                            slug         VARCHAR(350) NOT NULL UNIQUE,
                            thumbnail    VARCHAR(500),
                            excerpt      TEXT,
                            content      TEXT,
                            view_count   INT       NOT NULL DEFAULT 0,
                            is_published BOOLEAN   NOT NULL DEFAULT FALSE,
                            published_at TIMESTAMP,
                            meta_title   VARCHAR(300),
                            meta_desc    VARCHAR(500),
                            created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_blog_posts_category  ON blog_posts(category_id);
CREATE INDEX idx_blog_posts_published ON blog_posts(is_published, published_at DESC);
CREATE INDEX idx_blog_posts_slug      ON blog_posts(slug);

-- Liên kết bài viết ↔ sản phẩm đề cập (FR-27)
CREATE TABLE blog_post_products (
                                    post_id    INT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
                                    product_id INT NOT NULL REFERENCES products(id)   ON DELETE CASCADE,
                                    PRIMARY KEY (post_id, product_id)
);

-- ============================================================
-- PHẦN 17: BANNER & SLIDER (FR-28)
-- ============================================================

CREATE TABLE banners (
                         id         SERIAL    PRIMARY KEY,
                         title      VARCHAR(200),
                         image_url  VARCHAR(500) NOT NULL,
                         link_url   VARCHAR(500),
    -- FR-28: vị trí hiển thị
                         position   VARCHAR(50) NOT NULL DEFAULT 'home_slider', -- home_slider, sidebar, popup, brand
                         sort_order INT         NOT NULL DEFAULT 0,
                         start_date TIMESTAMP,
                         end_date   TIMESTAMP,
                         is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
                         created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_banners_active   ON banners(is_active, position);

-- ============================================================
-- PHẦN 18: WISHLIST & TƯƠNG TÁC (FR-24, FR-25)
-- ============================================================

-- Danh sách yêu thích (FR-24)
CREATE TABLE wishlists (
                           user_id    INT       NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
                           product_id INT       NOT NULL REFERENCES products(id)  ON DELETE CASCADE,
                           added_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                           PRIMARY KEY (user_id, product_id)
);
CREATE INDEX idx_wishlists_user ON wishlists(user_id);

-- Lịch sử xem sản phẩm (FR-25: tối đa 20 SP gần nhất)
CREATE TABLE product_views (
                               id         SERIAL    PRIMARY KEY,
                               product_id INT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                               user_id    INT       REFERENCES users(id),
                               session_id VARCHAR(100),
                               viewed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_product_views_user    ON product_views(user_id, viewed_at DESC);
CREATE INDEX idx_product_views_product ON product_views(product_id);

-- Lịch sử tìm kiếm (FR-05: lưu tối đa 10 từ khóa gần nhất)
CREATE TABLE search_history (
                                id         SERIAL    PRIMARY KEY,
                                user_id    INT       REFERENCES users(id) ON DELETE CASCADE,
                                session_id VARCHAR(100),
                                keyword    VARCHAR(200) NOT NULL,
                                result_count INT      NOT NULL DEFAULT 0,
                                searched_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                CONSTRAINT chk_search_owner CHECK (user_id IS NOT NULL OR session_id IS NOT NULL)
);
CREATE INDEX idx_search_history_user    ON search_history(user_id, searched_at DESC);
CREATE INDEX idx_search_history_session ON search_history(session_id, searched_at DESC);

-- ============================================================
-- PHẦN 19: THÔNG BÁO (FR-26)
-- ============================================================

CREATE TABLE notifications (
                               id             SERIAL            PRIMARY KEY,
                               user_id        INT               NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type           notification_type NOT NULL,
                               title          VARCHAR(200)      NOT NULL,
                               content        TEXT,
                               link_url       VARCHAR(500),
    -- Liên kết đến entity cụ thể (VD: order_id, service_request_id...)
                               reference_type VARCHAR(50),      -- 'order','return_request','service_request','warranty'
                               reference_id   INT,
                               is_read        BOOLEAN           NOT NULL DEFAULT FALSE,
                               created_at     TIMESTAMP         NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);
