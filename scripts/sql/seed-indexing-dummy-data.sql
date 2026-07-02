-- MySQL 8+ local/dev DB only.
-- Run reset-indexing-dummy-data.sql first.
--
-- Full mode default:
-- - members: 30,000 (29,990 MEMBER, 10 ADMIN)
-- - categories: 50
-- - products: 1,000,000
-- - orders: 1,000,000
-- - order_items: 2,000,000
--
-- Target login account:
-- - email: target@test.com
-- - raw password: Test1234!
-- Admin login accounts:
-- - email: admin01@test.com ~ admin10@test.com
-- - raw password: Test1234!

SET @target_email = 'target@test.com';
SET @dummy_password = '$2y$10$Ec9O2XWgpKzSXt1jaIO.yuIjdgAX4umqI8N1H4dWBrkvzvABkIKAy';

SET @member_count = 30000;
SET @admin_count = 10;
SET @category_count = 50;
SET @product_count = 1000000;
SET @order_count = 1000000;
SET @target_order_count = 40;
SET @items_per_order = 2;

SET @normal_member_count = @member_count - @admin_count - 1;
SET @normal_order_count = @order_count - @target_order_count;
SET @order_item_count = @order_count * @items_per_order;
SET @max_sequence = GREATEST(@member_count, @category_count, @product_count, @order_count, @order_item_count);

DROP TEMPORARY TABLE IF EXISTS seq;
CREATE TEMPORARY TABLE seq (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seq (n)
SELECT
    a.n
    + b.n * 10
    + c.n * 100
    + d.n * 1000
    + e.n * 10000
    + f.n * 100000
    + g.n * 1000000 AS n
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) f
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) g
WHERE
    a.n
    + b.n * 10
    + c.n * 100
    + d.n * 1000
    + e.n * 10000
    + f.n * 100000
    + g.n * 1000000 < @max_sequence;

INSERT INTO members (
    email,
    password,
    name,
    phone,
    role,
    created_at,
    updated_at
)
VALUES (
    @target_email,
    @dummy_password,
    'Target',
    '010-1234-5678',
    'MEMBER',
    NOW(),
    NOW()
);

INSERT INTO members (
    email,
    password,
    name,
    phone,
    role,
    created_at,
    updated_at
)
SELECT
    CONCAT('loadtest', LPAD(n + 1, 5, '0'), '@test.com'),
    @dummy_password,
    CONCAT('Load Test Member ', LPAD(n + 1, 5, '0')),
    CONCAT('010', LPAD(n + 1, 8, '0')),
    'MEMBER',
    NOW(),
    NOW()
FROM seq
WHERE n < @normal_member_count;

INSERT INTO members (
    email,
    password,
    name,
    phone,
    role,
    created_at,
    updated_at
)
SELECT
    CONCAT('admin', LPAD(n + 1, 2, '0'), '@test.com'),
    @dummy_password,
    CONCAT('Admin ', LPAD(n + 1, 2, '0')),
    CONCAT('019', LPAD(n + 1, 8, '0')),
    'ADMIN',
    NOW(),
    NOW()
FROM seq
WHERE n < @admin_count;

INSERT INTO categories (
    name,
    parent_id,
    created_at,
    updated_at
)
SELECT
    CONCAT('Category-', LPAD(n + 1, 2, '0')),
    NULL,
    NOW(),
    NOW()
FROM seq
WHERE n < @category_count;

SET @first_category_id = (
    SELECT MIN(id)
    FROM categories
);

INSERT INTO products (
    category_id,
    name,
    price,
    stock,
    status,
    description,
    created_at,
    updated_at
)
SELECT
    @first_category_id + (n % @category_count),
    CASE
        WHEN n % 1000 = 0 THEN CONCAT('게이밍 노트북 ', LPAD(n + 1, 7, '0'))
        WHEN n % 1000 = 1 THEN CONCAT('사무용 노트북 ', LPAD(n + 1, 7, '0'))
        WHEN n % 100 < 10 THEN CONCAT('무선 마우스 ', LPAD(n + 1, 7, '0'))
        WHEN n % 100 < 20 THEN CONCAT('기계식 키보드 ', LPAD(n + 1, 7, '0'))
        WHEN n % 100 < 30 THEN CONCAT('USB-C 허브 ', LPAD(n + 1, 7, '0'))
        WHEN n % 100 < 40 THEN CONCAT('텀블러 ', LPAD(n + 1, 7, '0'))
        WHEN n % 100 < 50 THEN CONCAT('운동화 ', LPAD(n + 1, 7, '0'))
        ELSE CONCAT('테스트 상품 ', LPAD(n + 1, 7, '0'))
    END,
    1000 + ((n % 2000) * 100),
    1000000,
    CASE
        WHEN n % 100 < 90 THEN 'ON_SALE'
        WHEN n % 100 < 98 THEN 'OUT_OF_STOCK'
        ELSE 'STOPPED'
    END,
    'Indexing dummy product',
    DATE_SUB(NOW(), INTERVAL (n % 1095) DAY),
    NOW()
FROM seq
WHERE n < @product_count;

SET @target_member_id = (
    SELECT id
    FROM members
    WHERE email = @target_email
);

SET @first_normal_member_id = (
    SELECT MIN(id)
    FROM members
    WHERE email LIKE 'loadtest%@test.com'
);

INSERT INTO orders (
    member_id,
    order_number,
    total_amount,
    order_status,
    pg_amount,
    paid_at,
    canceled_at,
    created_at,
    updated_at
)
SELECT
    @target_member_id,
    CONCAT('TARGET-ORDER-', LPAD(n + 1, 6, '0')),
    10000,
    order_status,
    10000,
    CASE
        WHEN order_status = 'PAID' THEN DATE_ADD(created_at, INTERVAL 10 MINUTE)
        ELSE NULL
    END,
    CASE
        WHEN order_status = 'CANCELLED' THEN DATE_ADD(created_at, INTERVAL 30 MINUTE)
        ELSE NULL
    END,
    created_at,
    NOW()
FROM (
    SELECT
        n,
        CASE
            WHEN n % 10 = 0 THEN 'CANCELLED'
            WHEN n % 10 < 8 THEN 'PAID'
            ELSE 'PENDING'
        END AS order_status,
        DATE_SUB(NOW(), INTERVAL n DAY) AS created_at
    FROM seq
    WHERE n < @target_order_count
) target_orders;

INSERT INTO orders (
    member_id,
    order_number,
    total_amount,
    order_status,
    pg_amount,
    paid_at,
    canceled_at,
    created_at,
    updated_at
)
SELECT
    member_id,
    CONCAT('ORD-', LPAD(n + 1, 12, '0')),
    10000,
    order_status,
    10000,
    CASE
        WHEN order_status = 'PAID' THEN DATE_ADD(created_at, INTERVAL 10 MINUTE)
        ELSE NULL
    END,
    CASE
        WHEN order_status = 'CANCELLED' THEN DATE_ADD(created_at, INTERVAL 30 MINUTE)
        ELSE NULL
    END,
    created_at,
    NOW()
FROM (
    SELECT
        n,
        @first_normal_member_id + (n % @normal_member_count) AS member_id,
        CASE
            WHEN n % 100 < 75 THEN 'PAID'
            WHEN n % 100 < 95 THEN 'PENDING'
            ELSE 'CANCELLED'
        END AS order_status,
        DATE_SUB(
            DATE_SUB(NOW(), INTERVAL (n % 1825) DAY),
            INTERVAL (n % 86400) SECOND
        ) AS created_at
    FROM seq
    WHERE n < @normal_order_count
) normal_orders;

DROP TEMPORARY TABLE IF EXISTS order_seq;
CREATE TEMPORARY TABLE order_seq (
    seq_n INT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    INDEX idx_order_seq_order_id (order_id)
) ENGINE = InnoDB;

INSERT INTO order_seq (
    seq_n,
    order_id
)
SELECT
    ROW_NUMBER() OVER (ORDER BY id) - 1,
    id
FROM orders;

DROP TEMPORARY TABLE IF EXISTS product_seq;
CREATE TEMPORARY TABLE product_seq (
    seq_n INT NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    INDEX idx_product_seq_product_id (product_id)
) ENGINE = InnoDB;

INSERT INTO product_seq (
    seq_n,
    product_id,
    name,
    price
)
SELECT
    ROW_NUMBER() OVER (ORDER BY id) - 1,
    id,
    name,
    price
FROM products;

SET @actual_order_count = (
    SELECT COUNT(*)
    FROM order_seq
);

SET @actual_product_count = (
    SELECT COUNT(*)
    FROM product_seq
);

INSERT INTO order_items (
    order_id,
    product_id,
    product_name_snapshot,
    product_price_snapshot,
    quantity,
    total_price
)
SELECT
    o.order_id,
    p.product_id,
    p.name,
    p.price,
    (s.n % 3) + 1,
    p.price * ((s.n % 3) + 1)
FROM seq s
JOIN order_seq o
    ON o.seq_n = FLOOR(s.n / @items_per_order)
JOIN product_seq p
    ON p.seq_n = ((s.n * 37 + FLOOR(s.n / @items_per_order)) % @actual_product_count)
WHERE s.n < @actual_order_count * @items_per_order;

UPDATE orders o
JOIN (
    SELECT
        order_id,
        SUM(total_price) AS total_amount
    FROM order_items
    GROUP BY order_id
) oi ON oi.order_id = o.id
SET
    o.total_amount = oi.total_amount,
    o.pg_amount = oi.total_amount;

ANALYZE TABLE members, categories, products, orders, order_items;

SELECT 'members' AS table_name, COUNT(*) AS row_count FROM members
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;
