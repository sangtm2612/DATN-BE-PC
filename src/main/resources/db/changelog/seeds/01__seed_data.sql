--liquibase formatted sql

--changeset kinhduanpc:seed-01 context:dev runOnChange:false splitStatements:true endDelimiter:;
--comment Seed data for development environment

-- ============================================================
-- PHẦN 21: DỮ LIỆU MẪU (Seed Data) — KinhDuanPC
-- ============================================================

-- ============ CATEGORIES ============
INSERT INTO categories (name, slug, sort_order, is_active) VALUES
                                                               ('Laptop',              'laptop',           1,  true),
                                                               ('PC Gaming',          'pc-gaming',        2,  true),
                                                               ('PC Văn phòng',       'pc-van-phong',     3,  true),
                                                               ('Linh kiện',          'linh-kien',        4,  true),
                                                               ('Màn hình',           'man-hinh',         5,  true),
                                                               ('Phím & Chuột',       'phim-chuot',       6,  true),
                                                               ('Tai nghe & Loa',     'tai-nghe-loa',     7,  true),
                                                               ('Tản nhiệt',          'tan-nhiet',        8,  true),
                                                               ('Mạng & Lưu trữ',    'mang-luu-tru',     9,  true),
                                                               ('Thiết bị văn phòng', 'thiet-bi-van-phong',10, true),
                                                               ('Camera an ninh',     'camera-an-ninh',   11, true),
                                                               ('Console & Game',     'console-game',     12, true),
                                                               ('Hàng cũ',            'hang-cu',          13, true),
                                                               ('Build PC',           'build-pc',         14, true)
    ON CONFLICT (slug) DO NOTHING;

-- Sub-categories for Linh kiện
INSERT INTO categories (parent_id, name, slug, sort_order, is_active)
SELECT c.id, sub.name, sub.slug, sub.sort_order, true
FROM categories c
         CROSS JOIN (VALUES
                         ('CPU',              'cpu',           1),
                         ('Mainboard',        'mainboard',     2),
                         ('RAM',              'ram',           3),
                         ('SSD',              'ssd',           4),
                         ('HDD',              'hdd',           5),
                         ('VGA',              'vga',           6),
                         ('Nguồn PSU',        'nguon-psu',     7),
                         ('Vỏ Case',          'vo-case',       8)
) AS sub(name, slug, sort_order)
WHERE c.slug = 'linh-kien'
    ON CONFLICT (slug) DO NOTHING;

-- ============ BRANDS ============
INSERT INTO brands (name, slug, is_active) VALUES
                                               ('ASUS',        'asus',        true),
                                               ('MSI',         'msi',         true),
                                               ('Gigabyte',    'gigabyte',    true),
                                               ('Intel',       'intel',       true),
                                               ('AMD',         'amd',         true),
                                               ('Samsung',     'samsung',     true),
                                               ('WD',          'wd',          true),
                                               ('Kingston',    'kingston',    true),
                                               ('Corsair',     'corsair',     true),
                                               ('Seagate',     'seagate',     true),
                                               ('LG',          'lg',          true),
                                               ('Dell',        'dell',        true),
                                               ('HP',          'hp',          true),
                                               ('Lenovo',      'lenovo',      true),
                                               ('Acer',        'acer',        true),
                                               ('Apple',       'apple',       true),
                                               ('Razer',       'razer',       true),
                                               ('Logitech',    'logitech',    true),
                                               ('SteelSeries', 'steelseries', true),
                                               ('HyperX',      'hyperx',      true),
                                               ('Cooler Master','cooler-master',true),
                                               ('be quiet!',   'be-quiet',    true),
                                               ('NZXT',        'nzxt',        true),
                                               ('Thermaltake', 'thermaltake', true),
                                               ('Lian Li',     'lian-li',     true)
    ON CONFLICT (slug) DO NOTHING;

-- ============ PRODUCTS (Sample) ============
INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, original_price, is_on_sale, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'laptop'),
    (SELECT id FROM brands WHERE slug = 'asus'),
    'ASUS ROG Strix G16 (2024) G614JV',
    'asus-rog-strix-g16-2024-g614jv',
    'ROG-G16-G614JV',
    'Laptop gaming mạnh mẽ với Intel Core i7-13650HX, NVIDIA RTX 4060, màn hình 165Hz',
    31990000, 35990000, true, 15, 24, '24 tháng chính hãng ASUS',
    true, true, true,
    'https://product.hstatic.net/200000722513/product/asus-rog-strix-g16_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'asus-rog-strix-g16-2024-g614jv');

INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, original_price, is_on_sale, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'laptop'),
    (SELECT id FROM brands WHERE slug = 'msi'),
    'MSI Katana 15 B13VEK',
    'msi-katana-15-b13vek',
    'MSI-KAT15-B13VEK',
    'Laptop gaming Intel Core i7-13620H, RTX 4050 6GB, 15.6" FHD 144Hz',
    22990000, 25990000, true, 20, 24, '24 tháng chính hãng MSI',
    true, true, true,
    'https://product.hstatic.net/200000722513/product/msi-katana-15_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'msi-katana-15-b13vek');

INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, original_price, is_on_sale, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'laptop'),
    (SELECT id FROM brands WHERE slug = 'lenovo'),
    'Lenovo LOQ 15IRX9 (2024)',
    'lenovo-loq-15irx9-2024',
    'LEN-LOQ15-IRX9',
    'Laptop gaming Intel Core i7-13650HX, RTX 4060 8GB, 15.6" FHD 144Hz, RAM 16GB',
    26990000, 29990000, true, 10, 24, '24 tháng chính hãng Lenovo',
    true, true, false,
    'https://product.hstatic.net/200000722513/product/lenovo-loq-15_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'lenovo-loq-15irx9-2024');

-- CPU Products
INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'cpu'),
    (SELECT id FROM brands WHERE slug = 'intel'),
    'Intel Core i5-13400F Tray',
    'intel-core-i5-13400f-tray',
    'INT-I5-13400F-TRAY',
    '10 nhân 16 luồng, Turbo 4.6GHz, Socket LGA1700',
    3890000, 10, 36, '36 tháng chính hãng',
    true, false, false,
    'https://product.hstatic.net/200000722513/product/intel-i5-13400f_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'intel-core-i5-13400f-tray');

INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'cpu'),
    (SELECT id FROM brands WHERE slug = 'amd'),
    'AMD Ryzen 5 7600X Tray',
    'amd-ryzen-5-7600x-tray',
    'AMD-R5-7600X-TRAY',
    '6 nhân 12 luồng, Boost 5.3GHz, Socket AM5',
    4190000, 8, 36, '36 tháng chính hãng',
    true, false, false,
    'https://product.hstatic.net/200000722513/product/amd-ryzen5-7600x_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'amd-ryzen-5-7600x-tray');

-- Monitor
INSERT INTO products (category_id, brand_id, name, slug, sku, short_desc, price, stock_qty, warranty_months, warranty_text, is_active, is_featured, is_new, thumbnail)
SELECT
    (SELECT id FROM categories WHERE slug = 'man-hinh'),
    (SELECT id FROM brands WHERE slug = 'lg'),
    'LG UltraGear 27GP850-B 27" QHD 165Hz',
    'lg-ultragear-27gp850-b',
    'LG-27GP850B',
    'Màn hình gaming 27", 2560x1440, IPS, 165Hz, 1ms GTG, G-Sync Compatible',
    7490000, 12, 24, '24 tháng chính hãng LG',
    true, true, false,
    'https://product.hstatic.net/200000722513/product/lg-27gp850_thumb.jpg'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE slug = 'lg-ultragear-27gp850-b');

-- ============ PC COMPONENT TYPES ============
INSERT INTO pc_component_types (name, slug, is_required, sort_order) VALUES
                                                                         ('CPU — Bộ vi xử lý',          'cpu',              true,  1),
                                                                         ('Mainboard — Bo mạch chủ',     'mainboard',        true,  2),
                                                                         ('RAM',                          'ram',              true,  3),
                                                                         ('SSD',                          'ssd',              true,  4),
                                                                         ('HDD',                          'hdd',              false, 5),
                                                                         ('VGA — Card màn hình',         'vga',              false, 6),
                                                                         ('VGA Like New',                'vga-like-new',     false, 7),
                                                                         ('Nguồn PSU',                   'nguon-psu',        true,  8),
                                                                         ('Vỏ Case',                     'vo-case',          true,  9),
                                                                         ('Màn hình',                    'man-hinh',         false,10),
                                                                         ('Bộ bàn phím + chuột',        'combo-phim-chuot', false,11),
                                                                         ('Bàn phím',                    'ban-phim',         false,12),
                                                                         ('Chuột',                       'chuot',            false,13),
                                                                         ('Tai nghe',                    'tai-nghe',         false,14),
                                                                         ('Loa',                         'loa',              false,15),
                                                                         ('Ghế Gaming',                  'ghe-gaming',       false,16),
                                                                         ('Quạt làm mát',                'quat-lam-mat',     false,17),
                                                                         ('Tản nhiệt khí',               'tan-nhiet-khi',    false,18),
                                                                         ('Tản nhiệt nước AIO',          'tan-nhiet-aio',    false,19),
                                                                         ('Tản nhiệt nước Custom',       'tan-nhiet-custom', false,20),
                                                                         ('Thiết bị mạng',               'thiet-bi-mang',    false,21),
                                                                         ('Windows bản quyền',           'windows',          false,22),
                                                                         ('Phần mềm Antivirus',          'antivirus',        false,23)
    ON CONFLICT (slug) DO NOTHING;

-- ============ STORES (21 showrooms) ============
INSERT INTO stores (name, slug, address, province, phone, open_hours, is_active) VALUES
                                                                                     ('KinhDuanPC Cầu Giấy',         'cau-giay',      '193 Cầu Giấy, P.Quan Hoa, Q.Cầu Giấy',          'Hà Nội',       '024.3795.1234', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Đống Đa',          'dong-da',       '121 Thái Hà, P.Trung Liệt, Q.Đống Đa',           'Hà Nội',       '024.3538.5678', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Hoàng Mai',        'hoang-mai',     'Tầng 1 TTTM HiPT, 102 Trường Chinh',             'Hà Nội',       '024.6261.9012', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Hà Đông',          'ha-dong',       '82 Nguyễn Trãi, P.Nguyễn Trãi, Q.Hà Đông',      'Hà Nội',       '024.3394.3456', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Long Biên',        'long-bien',     '289 Ngô Gia Tự, P.Đức Giang, Q.Long Biên',      'Hà Nội',       '024.6269.7890', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Hải Phòng',        'hai-phong',     '97 Tô Hiệu, Q.Lê Chân',                         'Hải Phòng',    '0225.382.1234', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Bắc Ninh',        'bac-ninh',      'Số 6 Lý Thái Tổ, TP.Bắc Ninh',                  'Bắc Ninh',     '0222.382.5678', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Thái Nguyên',      'thai-nguyen',   '286 Bắc Kạn, P.Túc Duyên, TP.Thái Nguyên',     'Thái Nguyên',  '0208.382.9012', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Đà Nẵng',         'da-nang',       '264 Tôn Đức Thắng, Q.Liên Chiểu',               'Đà Nẵng',      '0236.382.3456', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Huế',              'hue',           '26 Hùng Vương, TP.Huế',                          'Thừa Thiên Huế','0234.382.7890', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Quảng Nam',       'quang-nam',     '15 Trần Phú, TP.Tam Kỳ',                        'Quảng Nam',    '0235.382.1234', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Quy Nhơn',        'quy-nhon',      '283 Nguyễn Thái Học, TP.Quy Nhơn',              'Bình Định',    '0256.382.5678', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Nha Trang',        'nha-trang',     '54 Lê Thánh Tôn, P.Vĩnh Nguyên',               'Khánh Hòa',    '0258.382.9012', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Quận 1 HCM',      'q1-hcm',        '63 Trần Hưng Đạo, P.Nguyễn Cư Trinh, Q.1',     'TP. Hồ Chí Minh','028.3895.3456', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Quận 3 HCM',      'q3-hcm',        '97 Nguyễn Đình Chiểu, P.3, Q.3',               'TP. Hồ Chí Minh','028.3895.7890', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Gò Vấp HCM',      'go-vap-hcm',    '400 Nguyễn Kiệm, P.9, Q.Gò Vấp',              'TP. Hồ Chí Minh','028.3895.1234', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Bình Dương',       'binh-duong',    '190 Đại lộ Bình Dương, P.Phú Hòa, TP.TDM',    'Bình Dương',   '0274.382.5678', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Đồng Nai',        'dong-nai',      '188 Phạm Văn Thuận, P.Thống Nhất, TP.Biên Hòa','Đồng Nai',     '0251.382.9012', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Cần Thơ',         'can-tho',       '68 3/2, P.Hưng Lợi, Q.Ninh Kiều',              'Cần Thơ',      '0292.382.3456', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC Kiên Giang',      'kien-giang',    '101 Mạc Cửu, P.Vĩnh Thanh Vân, TP.Rạch Giá',  'Kiên Giang',   '0297.382.7890', '8:00 - 21:00', true),
                                                                                     ('KinhDuanPC An Giang',        'an-giang',      '28 Nguyễn Huệ, P.Mỹ Bình, TP.Long Xuyên',     'An Giang',     '0296.382.1234', '8:00 - 21:00', true)
    ON CONFLICT (slug) DO NOTHING;

-- ============ BANNERS ============
INSERT INTO banners (title, image_url, link_url, position, sort_order, is_active)
VALUES
    ('Siêu sale laptop gaming - Giảm đến 20%',
     'https://product.hstatic.net/200000722513/collections/slide-laptop.jpg',
     '/category/laptop', 'home_slider', 1, true),
    ('Build PC - Giảm 50% CPU khi lắp đủ bộ',
     'https://product.hstatic.net/200000722513/collections/slide-buildpc.jpg',
     '/build-pc', 'home_slider', 2, true),
    ('Màn hình gaming 165Hz - Từ 5 triệu',
     'https://product.hstatic.net/200000722513/collections/slide-monitor.jpg',
     '/category/man-hinh', 'home_slider', 3, true);

-- ============ BLOG CATEGORIES ============
INSERT INTO blog_categories (name, slug, sort_order) VALUES
                                                         ('Tin khuyến mãi',    'tin-khuyen-mai',    1),
                                                         ('Review sản phẩm',   'review-san-pham',   2),
                                                         ('Hướng dẫn',         'huong-dan',         3),
                                                         ('Tin công nghệ',     'tin-cong-nghe',     4),
                                                         ('Sự kiện',           'su-kien',           5)
    ON CONFLICT (slug) DO NOTHING;

-- ============ SHIPPING METHODS ============
INSERT INTO shipping_methods (name, description, base_fee, free_threshold, estimated_days, is_active)
VALUES
    ('Giao hàng tiêu chuẩn', 'Giao toàn quốc trong 2-3 ngày',     30000, 5000000,  '2-3 ngày',   true),
    ('Giao hàng nhanh',      'Giao trong 1-2 ngày làm việc',       50000, 10000000, '1-2 ngày',   true),
    ('Giao hỏa tốc',         'Giao trong 4 giờ (nội thành HN/HCM)',80000, NULL,     '4 giờ',      true),
    ('Nhận tại cửa hàng',    'Đến lấy trực tiếp tại showroom',     0,     NULL,     'Ngay hôm nay',true);

-- ============ INSTALLMENT PLANS ============
INSERT INTO installment_plans (name, provider, months, interest_rate, min_order, is_active)
VALUES
    ('Trả góp 0% - 6 tháng — HomeCredit',   'HomeCredit', 6,  0, 3000000, true),
    ('Trả góp 0% - 12 tháng — HomeCredit',  'HomeCredit', 12, 0, 3000000, true),
    ('Trả góp 0% - 6 tháng — FE Credit',   'FE Credit',  6,  0, 3000000, true),
    ('Trả góp 0% - 12 tháng — FE Credit',  'FE Credit',  12, 0, 3000000, true),
    ('Trả góp 0% - 6 tháng — Shinhan',     'Shinhan',    6,  0, 3000000, true),
    ('Trả góp 0% - 12 tháng — Shinhan',    'Shinhan',    12, 0, 3000000, true),
    ('Trả góp 0% - 24 tháng — HSBC',       'HSBC',       24, 0, 5000000, true);

-- ============ PROMOTIONS ============
INSERT INTO promotions (promotion_type, name, description, discount_type, discount_value, min_order_value, start_date, end_date, is_active)
VALUES
    ('general'::promotion_type,    'Giảm giá laptop tháng 6',   'Ưu đãi đặc biệt tháng 6 cho laptop gaming', 'percent'::discount_type,      10, 0, NOW(), NOW() + INTERVAL '30 days', true),
    ('build_pc'::promotion_type,   'Build PC Summer 2026',       'Giảm tối đa 50% CPU khi lắp đủ bộ',        'percent'::discount_type,      30, 0, NOW(), NOW() + INTERVAL '60 days', true),
    ('flash_sale'::promotion_type, 'Flash Sale Cuối Tuần',       'Sale sốc mỗi cuối tuần',                   'percent'::discount_type,      15, 0, NOW(), NOW() + INTERVAL '2 days',  true);

-- ============ VOUCHERS (sample) ============
INSERT INTO vouchers (code, name, discount_type, discount_value, min_order_value, max_discount, usage_limit, usage_per_user, start_date, end_date, is_active)
VALUES
    ('WELCOME50K',  'Chào mừng thành viên mới',    'fixed_amount'::discount_type,  50000,   500000,  NULL,    500,  1, NOW(), NOW() + INTERVAL '30 days', true),
    ('SALE10',      'Giảm 10% toàn bộ đơn hàng',  'percent'::discount_type,       10,      1000000, 200000, 1000,  1, NOW(), NOW() + INTERVAL '30 days', true),
    ('FREESHIP',    'Miễn phí vận chuyển',          'free_shipping'::discount_type, 0,       0,       NULL,   NULL,  1, NOW(), NOW() + INTERVAL '30 days', true),
    ('GAMING200K',  'Giảm 200K đơn gaming',        'fixed_amount'::discount_type,  200000,  5000000, NULL,    200,  1, NOW(), NOW() + INTERVAL '15 days', true)
    ON CONFLICT (code) DO NOTHING;

-- ============ ADMIN USER ============
-- Password: Admin@123 (bcrypt hash)
INSERT INTO users (role, status, email, phone, password_hash, full_name, email_verified, phone_verified)
VALUES (
           'admin'::user_role,
           'active'::user_status,
           'admin@kinhduanpc.vn',
           '0900000000',
           '$2b$12$xEg6WcusPpkoC6zBiCLLJutw0wYhh.n3Q4hBs4osnM658W9ChFSsi',
           'Quản trị viên',
           true, true
       )
    ON CONFLICT (email) DO NOTHING;
