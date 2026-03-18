-- ============================================================
-- 대량 데이터 시딩 스크립트 (10만건+ 상품)
-- 용도: 인덱스/캐시 성능 테스트를 위한 현실적 데이터 분포
-- ============================================================
-- 데이터 분포:
--   Brand: 50개
--   Product: 10만건 (상위 5개 브랜드에 5만건 집중, 나머지 45개에 5만건)
--   likeCount: 멱법칙 분포 (대부분 0~100, 소수가 만 단위)
--   price: 1,000~1,000,000 균등 분포
--   deleted: ~500건 (0.5%)
-- ============================================================

USE loopers;

-- 기존 시딩 데이터 정리 (테스트 데이터만)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE product_stocks;
TRUNCATE TABLE likes;
TRUNCATE TABLE products;
TRUNCATE TABLE brands;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. Brand 50개 생성
-- ============================================================
DROP PROCEDURE IF EXISTS seed_brands;

DELIMITER //
CREATE PROCEDURE seed_brands()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 50 DO
        INSERT INTO brands (name, created_at, updated_at, deleted_at)
        VALUES (
            CONCAT('Brand_', LPAD(i, 3, '0')),
            NOW() - INTERVAL (50 - i) DAY,
            NOW() - INTERVAL (50 - i) DAY,
            NULL
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_brands();
DROP PROCEDURE IF EXISTS seed_brands;

-- brand_id 범위 확인용
SET @min_brand_id = (SELECT MIN(id) FROM brands);
SET @max_brand_id = (SELECT MAX(id) FROM brands);

-- ============================================================
-- 2. Product 10만건 배치 INSERT
-- ============================================================
-- brand_id 분포:
--   상위 5개 브랜드 (id 1~5): 각 1만건 = 5만건 (선택도 낮음, 인덱스 효과 확인)
--   나머지 45개 브랜드 (id 6~50): 각 ~1,111건 = 5만건 (선택도 높음)
-- ============================================================

DROP PROCEDURE IF EXISTS seed_products;

DELIMITER //
CREATE PROCEDURE seed_products()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total INT DEFAULT 100000;
    DECLARE v_brand_id BIGINT;
    DECLARE v_like_count BIGINT;
    DECLARE v_price BIGINT;
    DECLARE v_deleted_at DATETIME(6);
    DECLARE v_created_at DATETIME(6);

    WHILE i < total DO
        -- 10만건 중 앞 5만건은 brand 1~5, 뒤 5만건은 brand 6~50
        IF i < 50000 THEN
            SET v_brand_id = @min_brand_id + (i DIV 10000);  -- 0~4 → brand 1~5
        ELSE
            SET v_brand_id = @min_brand_id + 5 + ((i - 50000) MOD 45);  -- brand 6~50
        END IF;

        -- likeCount: 멱법칙 분포
        -- RAND()^3 으로 대부분 작은 값, 소수만 큰 값
        SET v_like_count = FLOOR(POW(RAND(), 3) * 50000);

        -- price: 1,000 ~ 1,000,000 균등 분포 (1000 단위 반올림)
        SET v_price = (FLOOR(RAND() * 1000) + 1) * 1000;

        -- deleted: 0.5% 확률
        IF RAND() < 0.005 THEN
            SET v_deleted_at = NOW();
        ELSE
            SET v_deleted_at = NULL;
        END IF;

        -- created_at: 최근 365일 내 분포
        SET v_created_at = NOW() - INTERVAL FLOOR(RAND() * 365) DAY
                               - INTERVAL FLOOR(RAND() * 86400) SECOND;

        INSERT INTO products (brand_id, name, description, price, image_url, like_count, created_at, updated_at, deleted_at)
        VALUES (
            v_brand_id,
            CONCAT('Product_', LPAD(i + 1, 6, '0')),
            CONCAT('상품 설명 텍스트 #', i + 1, '. 이 상품은 테스트 데이터입니다.'),
            v_price,
            CONCAT('https://example.com/images/product_', i + 1, '.jpg'),
            v_like_count,
            v_created_at,
            v_created_at,
            v_deleted_at
        );

        SET i = i + 1;

        -- 진행률 로그 (1만건마다)
        IF i MOD 10000 = 0 THEN
            SELECT CONCAT('Inserted ', i, ' / ', total, ' products') AS progress;
        END IF;
    END WHILE;
END //
DELIMITER ;

CALL seed_products();
DROP PROCEDURE IF EXISTS seed_products;

-- ============================================================
-- 3. ProductStock 10만건 (product 1:1)
-- ============================================================
INSERT INTO product_stocks (product_id, stock)
SELECT id, FLOOR(RAND() * 500) + 1
FROM products;

-- ============================================================
-- 4. 데이터 분포 검증
-- ============================================================
SELECT '=== Data Distribution ===' AS info;

SELECT COUNT(*) AS total_brands FROM brands;
SELECT COUNT(*) AS total_products FROM products;
SELECT COUNT(*) AS total_stocks FROM product_stocks;
SELECT COUNT(*) AS active_products FROM products WHERE deleted_at IS NULL;
SELECT COUNT(*) AS deleted_products FROM products WHERE deleted_at IS NOT NULL;

SELECT brand_id, COUNT(*) AS cnt
FROM products WHERE deleted_at IS NULL
GROUP BY brand_id ORDER BY cnt DESC LIMIT 10;

SELECT
    MIN(like_count) AS min_likes,
    MAX(like_count) AS max_likes,
    AVG(like_count) AS avg_likes,
    COUNT(CASE WHEN like_count < 100 THEN 1 END) AS under_100,
    COUNT(CASE WHEN like_count >= 10000 THEN 1 END) AS over_10k
FROM products WHERE deleted_at IS NULL;

SELECT '=== Seeding Complete ===' AS info;
