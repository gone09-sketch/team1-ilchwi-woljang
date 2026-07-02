-- MySQL 8+ local/dev DB only.
-- Applies commerce category hierarchy to an existing indexing dummy dataset.
-- This does not truncate members/orders/order_items.

SET NAMES utf8mb4;

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
WHERE NOT EXISTS (
    SELECT 1
    FROM categories c
    WHERE c.name = roots.root_name
      AND c.parent_id IS NULL
)
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
WHERE NOT EXISTS (
    SELECT 1
    FROM categories child
    WHERE child.name = cs.child_name
      AND child.parent_id = root.id
)
ORDER BY cs.root_order, cs.child_order;

DROP TEMPORARY TABLE IF EXISTS product_category_map;
CREATE TEMPORARY TABLE product_category_map AS
SELECT
    ROW_NUMBER() OVER (ORDER BY cs.root_order, cs.child_order) - 1 AS rn,
    MIN(child.id) AS category_id,
    cs.root_name,
    cs.child_name
FROM category_seed cs
JOIN categories root
    ON root.name = cs.root_name
   AND root.parent_id IS NULL
JOIN categories child
    ON child.name = cs.child_name
   AND child.parent_id = root.id
GROUP BY
    cs.root_order,
    cs.child_order,
    cs.root_name,
    cs.child_name
ORDER BY cs.root_order, cs.child_order;

ALTER TABLE product_category_map
    ADD PRIMARY KEY (rn);

SET @leaf_category_count = (
    SELECT COUNT(*)
    FROM product_category_map
);

UPDATE products p
JOIN product_category_map pcm
    ON pcm.rn = CASE
        WHEN p.name LIKE '%노트북%' THEN 0
        WHEN p.name LIKE '%마우스%' OR p.name LIKE '%키보드%' THEN 1
        WHEN p.name LIKE '%USB-C 허브%' THEN 2
        WHEN p.name LIKE '%텀블러%' THEN 4
        WHEN p.name LIKE '%운동화%' THEN 12
        ELSE p.id % @leaf_category_count
    END
SET
    p.category_id = pcm.category_id,
    p.name = CASE
        WHEN p.name LIKE '테스트 상품 %' THEN CONCAT(
            CASE pcm.child_name
                WHEN '노트북' THEN '초경량 노트북'
                WHEN 'PC주변기기' THEN '모니터 받침대'
                WHEN '모바일/액세서리' THEN '고속 충전기'
                WHEN '청소용품' THEN '다용도 세정제'
                WHEN '주방용품' THEN '키친타월'
                WHEN '욕실용품' THEN '욕실 청소 브러시'
                WHEN '스킨케어' THEN '보습 크림'
                WHEN '메이크업' THEN '쿠션 파운데이션'
                WHEN '헤어/바디' THEN '바디로션'
                WHEN '건강식품' THEN '멀티비타민'
                WHEN '과자/간식' THEN '견과 믹스'
                WHEN '음료' THEN '우유 1L'
                WHEN '신발' THEN '러닝화'
                WHEN '가방/소품' THEN '데일리 백팩'
                ELSE '요가매트'
            END,
            ' ',
            LPAD(p.id, 7, '0')
        )
        ELSE p.name
    END,
    p.description = CONCAT(pcm.root_name, ' > ', pcm.child_name, ' 카테고리 인덱싱 더미 상품'),
    p.updated_at = NOW();

DELETE old_category
FROM categories old_category
LEFT JOIN products p
    ON p.category_id = old_category.id
LEFT JOIN categories child
    ON child.parent_id = old_category.id
WHERE old_category.name LIKE 'Category-%'
  AND p.id IS NULL
  AND child.id IS NULL;

ANALYZE TABLE categories, products;

SELECT
    root.name AS root_category,
    child.name AS child_category,
    COUNT(p.id) AS product_count
FROM categories root
JOIN categories child
    ON child.parent_id = root.id
LEFT JOIN products p
    ON p.category_id = child.id
WHERE root.parent_id IS NULL
GROUP BY root.name, child.name
ORDER BY root.name, child.name;
