-- Local/dev DB only.
-- This script deletes dummy data from tables used by indexing tests.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE carts;
TRUNCATE TABLE inquiries;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE products;
TRUNCATE TABLE categories;
TRUNCATE TABLE members;

SET FOREIGN_KEY_CHECKS = 1;

