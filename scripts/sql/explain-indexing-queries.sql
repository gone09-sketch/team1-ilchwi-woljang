-- MySQL 8+ local/dev DB only.
-- Use after seed-indexing-dummy-data.sql.
-- Keep destructive index changes commented unless you are intentionally comparing before/after plans.

SET @target_member_id = (
    SELECT id
    FROM members
    WHERE email = 'target@test.com'
);

SET @sample_member_id = (
    SELECT member_id
    FROM orders
    GROUP BY member_id
    ORDER BY COUNT(*) DESC, member_id ASC
    LIMIT 1
);

SHOW INDEX FROM orders;
SHOW INDEX FROM products;
SHOW INDEX FROM order_items;

-- Before/after comparison, if needed:
-- ALTER TABLE orders DROP INDEX idx_orders_member_id_created_at_id;
-- ALTER TABLE orders ADD INDEX idx_orders_member_id_created_at_id (member_id, created_at DESC, id DESC);

EXPLAIN
SELECT *
FROM orders
WHERE member_id = @target_member_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT *
FROM orders
WHERE member_id = @sample_member_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT *
FROM orders
WHERE member_id = @sample_member_id
  AND order_status = 'PAID'
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT *
FROM orders
WHERE member_id = @sample_member_id
  AND created_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT oi.order_id
FROM order_items oi
WHERE LOWER(oi.product_name_snapshot) LIKE '%노트북%';

EXPLAIN
SELECT *
FROM orders
WHERE member_id = @sample_member_id
  AND (
      LOWER(order_number) LIKE '%노트북%'
      OR id IN (
          SELECT oi.order_id
          FROM order_items oi
          WHERE LOWER(oi.product_name_snapshot) LIKE '%노트북%'
      )
  )
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

-- Product indexing teammate examples. Add/remove indexes separately before comparing plans.
EXPLAIN
SELECT *
FROM products
WHERE status = 'ON_SALE'
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT *
FROM products
WHERE category_id = (
    SELECT MIN(id)
    FROM categories
)
  AND status = 'ON_SALE'
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

