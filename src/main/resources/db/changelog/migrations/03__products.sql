--liquibase formatted sql

--changeset kinhduanpc:03__products runOnChange:false splitStatements:true endDelimiter:;
--comment Create products and related tables

-- ============================================================
-- PHẦN 2: SẢN PHẨM
-- ============================================================

-- Bảng sản phẩm chính (FR-06, FR-07, FR-30)
CREATE TABLE products (
                          id                   SERIAL PRIMARY KEY,
                          category_id          INT           NOT NULL REFERENCES categories(id),
                          brand_id             INT           REFERENCES brands(id),
                          name                 VARCHAR(300)  NOT NULL,
                          slug                 VARCHAR(350)  NOT NULL UNIQUE,
                          sku                  VARCHAR(100)  UNIQUE,
                          short_desc           TEXT,
                          description          TEXT,
                          thumbnail            VARCHAR(500),
                          price                NUMERIC(15,2) NOT NULL DEFAULT 0,
                          original_price       NUMERIC(15,2),                        -- Giá trước giảm
                          is_on_sale           BOOLEAN       NOT NULL DEFAULT FALSE,  -- Auto-set bởi trigger
                          stock_qty            INT           NOT NULL DEFAULT 0,      -- Tổng tồn kho
                          sold_qty             INT           NOT NULL DEFAULT 0,
                          view_count           INT           NOT NULL DEFAULT 0,
                          rating_avg           NUMERIC(3,2)  NOT NULL DEFAULT 0,
                          rating_count         INT           NOT NULL DEFAULT 0,
                          weight               NUMERIC(8,3),                         -- kg
                          warranty_months      INT           NOT NULL DEFAULT 12,
                          warranty_text        VARCHAR(200),                         -- "12 tháng chính hãng"
                          low_stock_threshold  INT           NOT NULL DEFAULT 5,      -- Cảnh báo tồn kho thấp
                          is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
                          is_featured          BOOLEAN       NOT NULL DEFAULT FALSE,
                          is_new               BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Full-text search vector (FR-05)
                          search_vector        TSVECTOR,
                          meta_title           VARCHAR(300),
                          meta_desc            VARCHAR(500),
                          created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
                          updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_category     ON products(category_id);
CREATE INDEX idx_products_brand        ON products(brand_id);
CREATE INDEX idx_products_slug         ON products(slug);
CREATE INDEX idx_products_sku          ON products(sku);
CREATE INDEX idx_products_price        ON products(price);
CREATE INDEX idx_products_active       ON products(is_active);
CREATE INDEX idx_products_featured     ON products(is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_products_on_sale      ON products(is_on_sale)  WHERE is_on_sale  = TRUE;
CREATE INDEX idx_products_search       ON products USING GIN(search_vector);
-- Trigram index cho ILIKE search (FR-05 autocomplete)
CREATE INDEX idx_products_name_trgm    ON products USING GIN(name gin_trgm_ops);

-- Gallery ảnh sản phẩm (FR-07: tối thiểu 5 ảnh)
CREATE TABLE product_images (
                                id          SERIAL PRIMARY KEY,
                                product_id  INT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                image_url   VARCHAR(500) NOT NULL,
                                alt_text    VARCHAR(200),
                                sort_order  INT          NOT NULL DEFAULT 0,
                                is_primary  BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_product_images_product ON product_images(product_id);

-- Thông số kỹ thuật dạng key-value (FR-07: CPU, RAM, màn hình...)
CREATE TABLE product_attributes (
                                    id               SERIAL PRIMARY KEY,
                                    product_id       INT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                    attribute_group  VARCHAR(100),                   -- "Cấu hình", "Màn hình", "Pin"
                                    attribute_name   VARCHAR(100) NOT NULL,
                                    attribute_value  VARCHAR(500) NOT NULL,
                                    unit             VARCHAR(50),
                                    sort_order       INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_attrs_product ON product_attributes(product_id);

-- Tags
CREATE TABLE tags (
                      id    SERIAL PRIMARY KEY,
                      name  VARCHAR(100) NOT NULL UNIQUE,
                      slug  VARCHAR(120) NOT NULL UNIQUE
);
CREATE TABLE product_tags (
                              product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                              tag_id     INT NOT NULL REFERENCES tags(id)     ON DELETE CASCADE,
                              PRIMARY KEY (product_id, tag_id)
);

-- Sản phẩm liên quan (FR-07: tối đa 8 SP)
CREATE TABLE product_related (
                                 product_id         INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                 related_product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                 sort_order         INT NOT NULL DEFAULT 0,
                                 PRIMARY KEY (product_id, related_product_id),
                                 CHECK (product_id <> related_product_id)
);

-- ============================================================
-- PHẦN 3: BUILD PC (FR-08 — tính năng đặc trưng HACOM)
-- ============================================================

-- 23 loại linh kiện có thể chọn (theo thực tế hacom.vn/buildpc)
CREATE TABLE pc_component_types (
                                    id          SERIAL PRIMARY KEY,
                                    name        VARCHAR(100) NOT NULL,
                                    slug        VARCHAR(120) NOT NULL UNIQUE,
                                    is_required BOOLEAN      NOT NULL DEFAULT FALSE,
                                    sort_order  INT          NOT NULL DEFAULT 0
);

-- Thông tin kỹ thuật linh kiện (hỗ trợ kiểm tra tương thích)
CREATE TABLE pc_components (
                               id                SERIAL PRIMARY KEY,
                               component_type_id INT          NOT NULL REFERENCES pc_component_types(id),
                               product_id        INT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    -- CPU & Mainboard compatibility
                               socket            VARCHAR(50),        -- LGA1700, AM5, AM4...
    -- Mainboard
                               chipset           VARCHAR(50),        -- Z790, B650, X670...
                               ram_type          VARCHAR(20),        -- DDR4, DDR5
                               ram_slots         INT,
                               max_ram_gb        INT,
    -- CPU / GPU TDP (để check PSU đủ công suất — FR-08)
                               tdp_watts         INT,
    -- PSU
                               psu_wattage       INT,                -- Công suất nguồn (W)
    -- VGA Like New flag (thực tế hacom.vn có mục VGA Like New)
                               is_like_new       BOOLEAN      NOT NULL DEFAULT FALSE,
    -- RAM
                               ram_capacity_gb   INT,                -- Dung lượng RAM
                               ram_speed_mhz     INT,
    -- SSD/HDD
                               storage_gb        INT,
                               storage_interface VARCHAR(30),        -- NVMe, SATA, PCIe 4.0
                               is_active         BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_pc_components_type    ON pc_components(component_type_id);
CREATE INDEX idx_pc_components_product ON pc_components(product_id);

-- Cấu hình PC đã lưu (của user hoặc template admin — UC-08)
CREATE TABLE pc_builds (
                           id            SERIAL PRIMARY KEY,
                           user_id       INT,
                           store_id      INT,                                -- Showroom đã chọn khi build
                           name          VARCHAR(200),
                           description   TEXT,
                           total_price   NUMERIC(15,2) NOT NULL DEFAULT 0,
                           discount_note TEXT,                               -- Ghi chú KM Build PC đang áp dụng
                           is_template   BOOLEAN       NOT NULL DEFAULT FALSE,
                           is_public     BOOLEAN       NOT NULL DEFAULT FALSE,
                           created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
                           updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pc_builds_user ON pc_builds(user_id);

-- Chi tiết linh kiện trong build
CREATE TABLE pc_build_items (
                                id                SERIAL PRIMARY KEY,
                                build_id          INT           NOT NULL REFERENCES pc_builds(id) ON DELETE CASCADE,
                                component_type_id INT           NOT NULL REFERENCES pc_component_types(id),
                                product_id        INT           NOT NULL REFERENCES products(id),
                                quantity          INT           NOT NULL DEFAULT 1 CHECK (quantity > 0),
                                unit_price        NUMERIC(15,2) NOT NULL DEFAULT 0
);
CREATE INDEX idx_pc_build_items_build ON pc_build_items(build_id);
