-- 성능 테스트용 대량 데이터 생성 스크립트
-- 상품 100만개, 좋아요 100만개

-- 1. 브랜드 데이터 생성 (100개)
INSERT INTO brands (name, description, created_at, updated_at, deleted_at)
SELECT
    CONCAT('Brand_', LPAD(numbers.n, 3, '0')) as name,
    CONCAT('Description for Brand ', numbers.n) as description,
    NOW() as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT @num := @num + 1 as n
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t3,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t4,
         (SELECT @num := -1) init
    LIMIT 100
) numbers
WHERE n < 100
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. 사용자 데이터 생성 (1000개)
INSERT INTO users (login_id, password, name, birth_date, email, created_at, updated_at, deleted_at)
SELECT
    CONCAT('user_', LPAD(numbers.n, 5, '0')) as login_id,
    '$2a$10$8yw/Yq8qQwAqAYcqQwAqAYcqQwAqAYcqQwAqAYcqQwAqAYcqQwAqA' as password,
    CONCAT('User ', numbers.n) as name,
    DATE_SUB(CURDATE(), INTERVAL numbers.n DAY) as birth_date,
    CONCAT('user', numbers.n, '@example.com') as email,
    NOW() as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT @num := @num + 1 as n
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @num := -1) init
    LIMIT 1000
) numbers
WHERE n < 1000
ON DUPLICATE KEY UPDATE login_id = VALUES(login_id);

-- 3. 상품 데이터 생성 (100만개)
-- 배치 단위로 INSERT하기 위해 프로시저 사용
DELIMITER $$

DROP PROCEDURE IF EXISTS insert_products$$

CREATE PROCEDURE insert_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_products INT DEFAULT 1000000;

    WHILE i <= total_products DO
        INSERT INTO products (brand_id, name, price, status, like_count, created_at, updated_at, deleted_at)
        SELECT
            MOD(seq.n - 1, 100) + 1 as brand_id,
            CONCAT('Product_', LPAD(seq.n, 7, '0')) as name,
            ROUND(RAND() * 1000000 + 1000, 2) as price,
            IF(MOD(seq.n, 5) = 0, 'INACTIVE', 'ACTIVE') as status,
            0 as like_count,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
            NOW() as updated_at,
            NULL as deleted_at
        FROM (
            SELECT @seq := @seq + 1 as n
            FROM (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t1,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t2,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t3,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t4,
            (SELECT @seq := i - 1) init
            LIMIT batch_size
        ) seq
        WHERE seq.n < total_products AND seq.n >= i;

        SET i = i + batch_size;
    END WHILE;
END$$

DELIMITER ;

-- 프로시저 실행 (100만개 상품 삽입)
CALL insert_products();

-- 4. 상품 좋아요 데이터 생성 (100만개)
-- 배치 단위로 INSERT하기 위해 프로시저 사용
DELIMITER $$

DROP PROCEDURE IF EXISTS insert_product_likes$$

CREATE PROCEDURE insert_product_likes()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_likes INT DEFAULT 1000000;

    WHILE i <= total_likes DO
        INSERT INTO product_likes (user_id, product_id, created_at, updated_at, deleted_at)
        SELECT
            MOD(seq.n - 1, 1000) + 1 as user_id,
            MOD(seq.n - 1, 1000000) + 1 as product_id,
            NOW() as created_at,
            NOW() as updated_at,
            NULL as deleted_at
        FROM (
            SELECT @seq := @seq + 1 as n
            FROM (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t1,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t2,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t3,
            (
                SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            ) t4,
            (SELECT @seq := i - 1) init
            LIMIT batch_size
        ) seq
        WHERE seq.n < total_likes AND seq.n >= i;

        SET i = i + batch_size;
    END WHILE;
END$$

DELIMITER ;

-- 프로시저 실행 (100만개 좋아요 삽입)
CALL insert_product_likes();

-- 5. 상품 좋아요 개수 업데이트
UPDATE products p
SET like_count = (
    SELECT COUNT(*)
    FROM product_likes pl
    WHERE pl.product_id = p.id
    AND pl.deleted_at IS NULL
)
WHERE p.deleted_at IS NULL;

-- 완료 메시지
SELECT CONCAT('Test data creation completed!') as status,
       (SELECT COUNT(*) FROM brands) as brand_count,
       (SELECT COUNT(*) FROM users) as user_count,
       (SELECT COUNT(*) FROM products WHERE deleted_at IS NULL) as product_count,
       (SELECT COUNT(*) FROM product_likes WHERE deleted_at IS NULL) as product_like_count;
