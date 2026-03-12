-- 브랜드 20개
INSERT INTO brands (name, description, created_at, updated_at) VALUES
('Brand_01', 'Brand 1 description', NOW(), NOW()),
('Brand_02', 'Brand 2 description', NOW(), NOW()),
('Brand_03', 'Brand 3 description', NOW(), NOW()),
('Brand_04', 'Brand 4 description', NOW(), NOW()),
('Brand_05', 'Brand 5 description', NOW(), NOW()),
('Brand_06', 'Brand 6 description', NOW(), NOW()),
('Brand_07', 'Brand 7 description', NOW(), NOW()),
('Brand_08', 'Brand 8 description', NOW(), NOW()),
('Brand_09', 'Brand 9 description', NOW(), NOW()),
('Brand_10', 'Brand 10 description', NOW(), NOW()),
('Brand_11', 'Brand 11 description', NOW(), NOW()),
('Brand_12', 'Brand 12 description', NOW(), NOW()),
('Brand_13', 'Brand 13 description', NOW(), NOW()),
('Brand_14', 'Brand 14 description', NOW(), NOW()),
('Brand_15', 'Brand 15 description', NOW(), NOW()),
('Brand_16', 'Brand 16 description', NOW(), NOW()),
('Brand_17', 'Brand 17 description', NOW(), NOW()),
('Brand_18', 'Brand 18 description', NOW(), NOW()),
('Brand_19', 'Brand 19 description', NOW(), NOW()),
('Brand_20', 'Brand 20 description', NOW(), NOW());

-- 상품 10만건 (Stored Procedure로 빠른 삽입)
DELIMITER //
CREATE PROCEDURE seed_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 100000 DO
        INSERT INTO products (brand_id, name, price, stock, description, image_url, like_count, created_at, updated_at)
        VALUES (
            (i % 20) + 1,
            CONCAT('Product_', LPAD(i, 6, '0')),
            ROUND(1000 + RAND() * 499000, 2),
            FLOOR(10 + RAND() * 990),
            CONCAT('상품 설명 ', i),
            CONCAT('https://img.example.com/', i, '.jpg'),
            FLOOR(RAND() * 10000),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_products();
DROP PROCEDURE seed_products;
