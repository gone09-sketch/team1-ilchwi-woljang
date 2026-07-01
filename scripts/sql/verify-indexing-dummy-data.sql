-- MySQL 8+ local/dev DB only.
-- Use after seed-indexing-dummy-data.sql.

SELECT 'members' AS table_name, COUNT(*) AS row_count FROM members
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

SELECT
    role,
    COUNT(*) AS member_count
FROM members
GROUP BY role
ORDER BY role;

SELECT
    status,
    COUNT(*) AS product_count
FROM products
GROUP BY status
ORDER BY status;

SELECT
    order_status,
    COUNT(*) AS order_count
FROM orders
GROUP BY order_status
ORDER BY order_status;

SELECT
    m.id,
    m.email,
    COUNT(o.id) AS order_count
FROM members m
LEFT JOIN orders o ON o.member_id = m.id
WHERE m.email = 'target@test.com'
GROUP BY m.id, m.email;

SELECT
    MAX(order_count) AS max_orders_per_member,
    MIN(order_count) AS min_orders_per_ordering_member,
    AVG(order_count) AS avg_orders_per_ordering_member
FROM (
    SELECT
        member_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY member_id
) member_order_counts;

SELECT
    member_id,
    COUNT(*) AS order_count
FROM orders
GROUP BY member_id
ORDER BY order_count DESC, member_id ASC
LIMIT 20;

SELECT
    COUNT(*) AS order_item_count,
    COUNT(DISTINCT order_id) AS order_count_with_items,
    COUNT(DISTINCT product_id) AS ordered_product_count
FROM order_items;

SELECT
    product_name_snapshot,
    COUNT(*) AS item_count
FROM order_items
WHERE product_name_snapshot LIKE '%노트북%'
GROUP BY product_name_snapshot
ORDER BY item_count DESC
LIMIT 20;

SELECT
    p.category_id,
    COUNT(*) AS product_count
FROM products p
GROUP BY p.category_id
ORDER BY product_count DESC, p.category_id ASC
LIMIT 20;

