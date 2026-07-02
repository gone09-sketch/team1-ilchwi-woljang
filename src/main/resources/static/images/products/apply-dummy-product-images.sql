-- products.image_url 컬럼이 없다면 먼저 추가
ALTER TABLE products
ADD COLUMN image_url VARCHAR(500) NULL;

-- 기존 더미 상품 데이터에 카테고리별 대표 이미지 5개를 반복 매핑
UPDATE products p
JOIN categories c ON p.category_id = c.id
SET p.image_url = CONCAT(
    '/images/products/',
    CASE c.name
        WHEN '노트북' THEN 'laptop'
        WHEN 'PC주변기기' THEN 'pc-accessory'
        WHEN '모바일/액세서리' THEN 'mobile'
        WHEN '청소용품' THEN 'cleaning'
        WHEN '주방용품' THEN 'kitchen'
        WHEN '욕실용품' THEN 'bathroom'
        WHEN '스킨케어' THEN 'skincare'
        WHEN '메이크업' THEN 'makeup'
        WHEN '헤어/바디' THEN 'hair-body'
        WHEN '건강식품' THEN 'health-food'
        WHEN '과자/간식' THEN 'snack'
        WHEN '음료' THEN 'drink'
        WHEN '신발' THEN 'shoes'
        WHEN '가방/소품' THEN 'bag'
        WHEN '스포츠' THEN 'sports'
        ELSE 'sports'
    END,
    '-',
    LPAD((p.id % 5) + 1, 2, '0'),
    '.png'
);
