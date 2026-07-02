-- MySQL 8+ local/dev DB only.
-- Run reset-indexing-dummy-data.sql first.
--
-- Full mode default:
-- - members: 30,000 (29,990 MEMBER, 10 ADMIN)
-- - categories: 20 (5 roots, 15 children)
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

SET NAMES utf8mb4;

SET @target_email = _utf8mb4'target@test.com' COLLATE utf8mb4_unicode_ci;
SET @dummy_password = '$2y$10$Ec9O2XWgpKzSXt1jaIO.yuIjdgAX4umqI8N1H4dWBrkvzvABkIKAy';

SET @member_count = 30000;
SET @admin_count = 10;
SET @category_count = 20;
SET @leaf_category_count = 15;
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

DROP TEMPORARY TABLE IF EXISTS category_seed;
CREATE TEMPORARY TABLE category_seed (
    root_order INT NOT NULL,
    child_order INT NOT NULL,
    root_name VARCHAR(50) NOT NULL,
    child_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (root_order, child_order)
) ENGINE = MEMORY;

INSERT INTO category_seed (
    root_order,
    child_order,
    root_name,
    child_name
)
VALUES
    (1, 1, '전자기기', '노트북'),
    (1, 2, '전자기기', 'PC주변기기'),
    (1, 3, '전자기기', '모바일/액세서리'),
    (2, 1, '생활용품', '청소용품'),
    (2, 2, '생활용품', '주방용품'),
    (2, 3, '생활용품', '욕실용품'),
    (3, 1, '뷰티', '스킨케어'),
    (3, 2, '뷰티', '메이크업'),
    (3, 3, '뷰티', '헤어/바디'),
    (4, 1, '식품', '건강식품'),
    (4, 2, '식품', '과자/간식'),
    (4, 3, '식품', '음료'),
    (5, 1, '패션/잡화', '신발'),
    (5, 2, '패션/잡화', '가방/소품'),
    (5, 3, '패션/잡화', '스포츠/레저');

INSERT INTO categories (
    name,
    parent_id,
    created_at,
    updated_at
)
SELECT
    roots.root_name,
    NULL,
    NOW(),
    NOW()
FROM (
    SELECT DISTINCT
        root_order,
        root_name
    FROM category_seed
) roots
ORDER BY roots.root_order;

INSERT INTO categories (
    name,
    parent_id,
    created_at,
    updated_at
)
SELECT
    cs.child_name,
    root.id,
    NOW(),
    NOW()
FROM category_seed cs
JOIN categories root
    ON root.name = cs.root_name
   AND root.parent_id IS NULL
ORDER BY cs.root_order, cs.child_order;

DROP TEMPORARY TABLE IF EXISTS product_category_map;
CREATE TEMPORARY TABLE product_category_map AS
SELECT
    ROW_NUMBER() OVER (ORDER BY cs.root_order, cs.child_order) - 1 AS rn,
    child.id AS category_id,
    cs.root_name,
    cs.child_name
FROM category_seed cs
JOIN categories root
    ON root.name = cs.root_name
   AND root.parent_id IS NULL
JOIN categories child
    ON child.name = cs.child_name
   AND child.parent_id = root.id
ORDER BY cs.root_order, cs.child_order;

ALTER TABLE product_category_map
    ADD PRIMARY KEY (rn);

SET @leaf_category_count = (
    SELECT COUNT(*)
    FROM product_category_map
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
    pcm.category_id,
    CONCAT(
        CASE pcm.child_name
            WHEN '노트북' THEN
                CASE n % 3
                    WHEN 0 THEN '게이밍 노트북'
                    WHEN 1 THEN '사무용 노트북'
                    ELSE '초경량 노트북'
                END
            WHEN 'PC주변기기' THEN
                CASE n % 3
                    WHEN 0 THEN '무선 마우스'
                    WHEN 1 THEN '기계식 키보드'
                    ELSE '모니터 받침대'
                END
            WHEN '모바일/액세서리' THEN
                CASE n % 3
                    WHEN 0 THEN 'USB-C 허브'
                    WHEN 1 THEN '고속 충전기'
                    ELSE '무선 이어폰'
                END
            WHEN '청소용품' THEN
                CASE n % 3
                    WHEN 0 THEN '다용도 세정제'
                    WHEN 1 THEN '프리미엄 물티슈'
                    ELSE '먼지 제거 클리너'
                END
            WHEN '주방용품' THEN
                CASE n % 3
                    WHEN 0 THEN '주방 수세미 세트'
                    WHEN 1 THEN '실리콘 조리도구'
                    ELSE '키친타월'
                END
            WHEN '욕실용품' THEN
                CASE n % 3
                    WHEN 0 THEN '욕실 청소 브러시'
                    WHEN 1 THEN '샤워타월'
                    ELSE '배수구 클리너'
                END
            WHEN '스킨케어' THEN
                CASE n % 3
                    WHEN 0 THEN '보습 크림'
                    WHEN 1 THEN '수분 에센스'
                    ELSE '선크림'
                END
            WHEN '메이크업' THEN
                CASE n % 3
                    WHEN 0 THEN '틴트 컬러밤'
                    WHEN 1 THEN '쿠션 파운데이션'
                    ELSE '아이브로우 펜슬'
                END
            WHEN '헤어/바디' THEN
                CASE n % 3
                    WHEN 0 THEN '바디로션'
                    WHEN 1 THEN '데일리 샴푸'
                    ELSE '헤어 트리트먼트'
                END
            WHEN '건강식품' THEN
                CASE n % 3
                    WHEN 0 THEN '저당 그래놀라'
                    WHEN 1 THEN '멀티비타민'
                    ELSE '단백질바'
                END
            WHEN '과자/간식' THEN
                CASE n % 3
                    WHEN 0 THEN '견과 믹스'
                    WHEN 1 THEN '감자칩'
                    ELSE '초코 쿠키'
                END
            WHEN '음료' THEN
                CASE n % 3
                    WHEN 0 THEN '우유 1L'
                    WHEN 1 THEN '콤부차'
                    ELSE '생수'
                END
            WHEN '신발' THEN
                CASE n % 3
                    WHEN 0 THEN '운동화'
                    WHEN 1 THEN '러닝화'
                    ELSE '슬리퍼'
                END
            WHEN '가방/소품' THEN
                CASE n % 3
                    WHEN 0 THEN '데일리 백팩'
                    WHEN 1 THEN '에코백'
                    ELSE '카드지갑'
                END
            ELSE
                CASE n % 3
                    WHEN 0 THEN '요가매트'
                    WHEN 1 THEN '스포츠 양말'
                    ELSE '트레이닝 밴드'
                END
        END,
        ' ',
        LPAD(n + 1, 7, '0')
    ),
    1000 + ((n % 2000) * 100),
    1000000,
    CASE
        WHEN n % 100 < 90 THEN 'ON_SALE'
        WHEN n % 100 < 98 THEN 'OUT_OF_STOCK'
        ELSE 'STOPPED'
    END,
    CONCAT(pcm.root_name, ' > ', pcm.child_name, ' 카테고리 인덱싱 더미 상품'),
    DATE_SUB(NOW(), INTERVAL (n % 1095) DAY),
    NOW()
FROM seq
JOIN product_category_map pcm
    ON pcm.rn = (n % @leaf_category_count)
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
