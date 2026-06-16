--liquibase formatted sql

--changeset kinhduanpc:08__triggers runOnChange:true splitStatements:false endDelimiter:--endchangeset--
--comment Create all database triggers

-- ============================================================
-- PHẦN 20: TRIGGERS
-- ============================================================

-- [T1] Auto cập nhật updated_at
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_categories_updated    BEFORE UPDATE ON categories    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_products_updated      BEFORE UPDATE ON products      FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_users_updated         BEFORE UPDATE ON users         FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_user_addresses_updated BEFORE UPDATE ON user_addresses FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_orders_updated        BEFORE UPDATE ON orders        FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_warranties_updated    BEFORE UPDATE ON warranties    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_service_updated       BEFORE UPDATE ON service_requests FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_return_updated        BEFORE UPDATE ON return_requests  FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_pc_builds_updated     BEFORE UPDATE ON pc_builds     FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_order_shipping_updated BEFORE UPDATE ON order_shipping FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_blog_posts_updated    BEFORE UPDATE ON blog_posts    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- [T2] Auto set is_on_sale khi giá thay đổi
CREATE OR REPLACE FUNCTION fn_products_on_sale()
RETURNS TRIGGER AS $$
BEGIN
    NEW.is_on_sale := (
        NEW.original_price IS NOT NULL AND NEW.price < NEW.original_price
    );
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_on_sale
    BEFORE INSERT OR UPDATE OF price, original_price ON products
    FOR EACH ROW EXECUTE FUNCTION fn_products_on_sale();

-- [T3] Auto build search_vector cho full-text search (FR-05)
CREATE OR REPLACE FUNCTION fn_products_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.name, ''))),       'A') ||
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.sku, ''))),        'B') ||
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.short_desc, ''))), 'C');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_search_vector
    BEFORE INSERT OR UPDATE OF name, sku, short_desc ON products
    FOR EACH ROW EXECUTE FUNCTION fn_products_search_vector();

-- [T4] Auto cập nhật rating_avg & rating_count khi có review
CREATE OR REPLACE FUNCTION fn_update_product_rating()
RETURNS TRIGGER AS $$
DECLARE
v_product_id INT;
BEGIN
    v_product_id := COALESCE(NEW.product_id, OLD.product_id);
UPDATE products
SET
    rating_avg   = COALESCE((
                                SELECT ROUND(AVG(rating)::NUMERIC, 2)
                                FROM reviews
                                WHERE product_id = v_product_id AND is_approved = TRUE
                            ), 0),
    rating_count = (
        SELECT COUNT(*)
        FROM reviews
        WHERE product_id = v_product_id AND is_approved = TRUE
    )
WHERE id = v_product_id;
RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_product_rating
    AFTER INSERT OR UPDATE OR DELETE ON reviews
    FOR EACH ROW EXECUTE FUNCTION fn_update_product_rating();

-- [T5] Auto trừ tồn kho khi đơn hàng được xác nhận
CREATE OR REPLACE FUNCTION fn_decrease_stock_on_confirm()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'confirmed' AND OLD.status = 'pending' THEN
        -- Trừ tổng tồn kho
UPDATE products p
SET stock_qty = stock_qty - oi.quantity,
    sold_qty  = sold_qty  + oi.quantity
    FROM order_items oi
WHERE oi.order_id = NEW.id AND oi.product_id = p.id;

-- Trừ tồn kho theo showroom (nếu có pickup_store)
IF NEW.pickup_store_id IS NOT NULL THEN
UPDATE product_stock_by_store psbs
SET stock_qty  = psbs.stock_qty - oi.quantity,
    updated_at = NOW()
    FROM order_items oi
WHERE oi.order_id = NEW.id
  AND psbs.product_id = oi.product_id
  AND psbs.store_id   = NEW.pickup_store_id;
END IF;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_decrease_stock
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION fn_decrease_stock_on_confirm();

-- [T6] Auto hoàn tồn kho khi đơn hàng bị hủy (FR-15)
CREATE OR REPLACE FUNCTION fn_restore_stock_on_cancel()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'cancelled'
       AND OLD.status IN ('pending', 'confirmed', 'processing')
    THEN
UPDATE products p
SET stock_qty = stock_qty + oi.quantity,
    sold_qty  = GREATEST(0, sold_qty - oi.quantity)
    FROM order_items oi
WHERE oi.order_id = NEW.id AND oi.product_id = p.id;

IF NEW.pickup_store_id IS NOT NULL THEN
UPDATE product_stock_by_store psbs
SET stock_qty  = psbs.stock_qty + oi.quantity,
    updated_at = NOW()
    FROM order_items oi
WHERE oi.order_id = NEW.id
  AND psbs.product_id = oi.product_id
  AND psbs.store_id   = NEW.pickup_store_id;
END IF;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restore_stock_on_cancel
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION fn_restore_stock_on_cancel();

-- [T7] Auto set auto_cancel_at cho đơn COD (FR-10: hủy sau 48h chưa xác nhận)
CREATE OR REPLACE FUNCTION fn_set_auto_cancel()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.payment_method = 'cod' AND NEW.status = 'pending' THEN
        NEW.auto_cancel_at := NOW() + INTERVAL '48 hours';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_auto_cancel
    BEFORE INSERT ON orders
    FOR EACH ROW EXECUTE FUNCTION fn_set_auto_cancel();

-- [T8] Auto cập nhật helpful_count trong reviews
CREATE OR REPLACE FUNCTION fn_update_helpful_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE reviews SET helpful_count = helpful_count + 1 WHERE id = NEW.review_id;
ELSIF TG_OP = 'DELETE' THEN
UPDATE reviews SET helpful_count = GREATEST(0, helpful_count - 1) WHERE id = OLD.review_id;
END IF;
RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_helpful_count
    AFTER INSERT OR DELETE ON review_helpful
    FOR EACH ROW EXECUTE FUNCTION fn_update_helpful_count();

-- [T9] Auto cập nhật tổng stock_qty từ product_stock_by_store
CREATE OR REPLACE FUNCTION fn_sync_total_stock()
RETURNS TRIGGER AS $$
BEGIN
UPDATE products
SET stock_qty = (
    SELECT COALESCE(SUM(stock_qty), 0)
    FROM product_stock_by_store
    WHERE product_id = NEW.product_id
)
WHERE id = NEW.product_id;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_total_stock
    AFTER INSERT OR UPDATE ON product_stock_by_store
                        FOR EACH ROW EXECUTE FUNCTION fn_sync_total_stock();
