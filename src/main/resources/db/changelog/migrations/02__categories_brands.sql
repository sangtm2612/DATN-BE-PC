--liquibase formatted sql

--changeset kinhduanpc:02__categories_brands runOnChange:false splitStatements:true endDelimiter:;
--comment Create categories and brands tables

-- ============================================================
-- PHẦN 1: DANH MỤC & THƯƠNG HIỆU
-- ============================================================

-- Cây danh mục đa cấp (FR-04)
CREATE TABLE categories (
                            id          SERIAL PRIMARY KEY,
                            parent_id   INT REFERENCES categories(id) ON DELETE SET NULL,
                            name        VARCHAR(150) NOT NULL,
                            slug        VARCHAR(200) NOT NULL UNIQUE,
                            icon_url    VARCHAR(500),
                            image_url   VARCHAR(500),
                            description TEXT,
                            sort_order  INT          NOT NULL DEFAULT 0,
                            is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                            updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug   ON categories(slug);

-- Thương hiệu (FR-04, FR-22)
CREATE TABLE brands (
                        id          SERIAL PRIMARY KEY,
                        name        VARCHAR(100) NOT NULL,
                        slug        VARCHAR(150) NOT NULL UNIQUE,
                        logo_url    VARCHAR(500),
                        website     VARCHAR(300),
                        description TEXT,
                        is_active   BOOLEAN   NOT NULL DEFAULT TRUE,
                        created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
