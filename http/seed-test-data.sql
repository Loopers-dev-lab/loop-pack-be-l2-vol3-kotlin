-- ============================================================
-- 테스트 데이터 시드 스크립트
-- 브랜드 20개 + 상품 10만 건 + 좋아요 20만 건
-- 실행: docker exec -i mysql mysql -u application -papplication loopers < http/seed-test-data.sql
-- ============================================================

-- 1. 브랜드 20개
INSERT INTO brands (name, description, image_url, created_at, updated_at)
SELECT
    CONCAT('Brand_', seq),
    CONCAT('Brand ', seq, ' description'),
    CONCAT('https://example.com/brand', seq, '.png'),
    NOW() - INTERVAL (21 - seq) DAY,
    NOW() - INTERVAL (21 - seq) DAY
FROM (
    SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
) AS nums;

-- 2. 상품 10만 건 (프로시저)
DROP PROCEDURE IF EXISTS seed_products;
DELIMITER $$
CREATE PROCEDURE seed_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_brand_id INT;
    DECLARE v_price BIGINT;
    DECLARE v_stock INT;
    DECLARE v_like_count INT;
    DECLARE v_status VARCHAR(20);
    DECLARE v_display_yn TINYINT;
    DECLARE v_deleted_at TIMESTAMP(6);
    DECLARE v_created_at TIMESTAMP(6);
    DECLARE v_rand DOUBLE;

    -- 배치 INSERT 성능을 위한 설정
    SET autocommit = 0;

    WHILE i <= 100000 DO
        SET v_brand_id = 1 + FLOOR(RAND() * 20);
        SET v_price = 5000 + FLOOR(RAND() * 495000);           -- 5,000 ~ 500,000
        SET v_stock = FLOOR(RAND() * 500);                      -- 0 ~ 499
        SET v_like_count = FLOOR(RAND() * RAND() * 10000);      -- 편향 분포 (대부분 낮고, 소수가 높음)
        SET v_created_at = NOW() - INTERVAL FLOOR(RAND() * 365) DAY
                                 - INTERVAL FLOOR(RAND() * 86400) SECOND;
        SET v_rand = RAND();

        -- 상태 분포: ACTIVE 85%, INACTIVE 10%, DELETED 5%
        IF v_rand < 0.85 THEN
            SET v_status = 'ACTIVE';
            SET v_display_yn = IF(RAND() < 0.9, 1, 0);         -- ACTIVE 중 90% 전시
            SET v_deleted_at = NULL;
        ELSEIF v_rand < 0.95 THEN
            SET v_status = 'INACTIVE';
            SET v_display_yn = 0;
            SET v_deleted_at = NULL;
        ELSE
            SET v_status = 'DELETED';
            SET v_display_yn = 0;
            SET v_deleted_at = NOW() - INTERVAL FLOOR(RAND() * 30) DAY;
        END IF;

        INSERT INTO products (brand_id, name, description, price, stock_quantity, like_count,
                              status, display_yn, image_url, created_at, updated_at, deleted_at)
        VALUES (v_brand_id,
                CONCAT('Product_', i),
                CONCAT('Description for product ', i),
                v_price,
                v_stock,
                v_like_count,
                v_status,
                v_display_yn,
                CONCAT('https://example.com/product', i, '.png'),
                v_created_at,
                v_created_at,
                v_deleted_at);

        -- 1만 건마다 커밋
        IF i % 10000 = 0 THEN
            COMMIT;
        END IF;

        SET i = i + 1;
    END WHILE;

    COMMIT;
END$$
DELIMITER ;

CALL seed_products();
DROP PROCEDURE IF EXISTS seed_products;

-- 3. 좋아요 20만 건 (유저 1~1000, 상품 랜덤)
DROP PROCEDURE IF EXISTS seed_likes;
DELIMITER $$
CREATE PROCEDURE seed_likes()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id BIGINT;
    DECLARE v_product_id BIGINT;
    DECLARE v_created_at TIMESTAMP(6);

    SET autocommit = 0;

    WHILE i <= 200000 DO
        SET v_user_id = 1 + FLOOR(RAND() * 1000);
        SET v_product_id = 1 + FLOOR(RAND() * 100000);
        SET v_created_at = NOW() - INTERVAL FLOOR(RAND() * 180) DAY;

        -- 중복 무시 (UK: user_id + product_id)
        INSERT IGNORE INTO likes (user_id, product_id, created_at, updated_at)
        VALUES (v_user_id, v_product_id, v_created_at, v_created_at);

        IF i % 10000 = 0 THEN
            COMMIT;
        END IF;

        SET i = i + 1;
    END WHILE;

    COMMIT;
END$$
DELIMITER ;

CALL seed_likes();
DROP PROCEDURE IF EXISTS seed_likes;

-- 4. 확인
SELECT 'products' AS tbl, COUNT(*) AS cnt FROM products
UNION ALL
SELECT 'likes', COUNT(*) FROM likes
UNION ALL
SELECT 'brands', COUNT(*) FROM brands;
