-- =============================================================
-- 테스트 데이터 생성 스크립트
-- brands: 50,000건, users: 100,000건, products: 1,000,000건
-- =============================================================

-- 성능 최적화 설정
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;
SET @saved_sql_mode = @@sql_mode;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
SET cte_max_recursion_depth = 10000;

-- =============================================================
-- 1. 시퀀스 테이블 생성 (숫자 생성용)
-- =============================================================
DROP TABLE IF EXISTS seq_helper;
CREATE TABLE seq_helper (n INT PRIMARY KEY);

-- 0~9999 까지 숫자 생성
INSERT INTO seq_helper (n)
WITH RECURSIVE cte AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 9999
)
SELECT n FROM cte;
COMMIT;

-- =============================================================
-- 2. Brands 생성 (50,000건)
-- =============================================================
-- 기존 데이터 삭제
DELETE FROM brands;
ALTER TABLE brands AUTO_INCREMENT = 1;

-- 50,000건 삽입 (5 배치 x 10,000건)
INSERT INTO brands (name, description, created_at, updated_at, deleted_at)
SELECT
    CONCAT('Brand_', (batch.b * 10000) + seq.n + 1),
    CONCAT('Description for brand ', (batch.b * 10000) + seq.n + 1),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(((batch.b * 10000) + seq.n) * 7 + 1) * 730) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(((batch.b * 10000) + seq.n) * 7 + 2) * 365) DAY),
    NULL
FROM seq_helper seq
CROSS JOIN (SELECT 0 AS b UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) batch
WHERE seq.n < 10000;
COMMIT;

SELECT COUNT(*) AS brand_count FROM brands;

-- =============================================================
-- 3. Users 생성 (100,000건)
-- =============================================================
DELETE FROM users;
ALTER TABLE users AUTO_INCREMENT = 1;

-- 100,000건 삽입 (10 배치 x 10,000건)
INSERT INTO users (login_id, password, name, birth, email, created_at, updated_at, deleted_at)
SELECT
    CONCAT('user', LPAD((batch.b * 10000) + seq.n + 1, 8, '0')),
    -- SHA-256 해시 시뮬레이션 (64자 hex)
    SHA2(CONCAT('password', (batch.b * 10000) + seq.n + 1), 256),
    CONCAT('User', (batch.b * 10000) + seq.n + 1),
    DATE_FORMAT(
        DATE_SUB('2000-01-01', INTERVAL FLOOR(RAND(((batch.b * 10000) + seq.n) * 11 + 1) * 10000) DAY),
        '%Y-%m-%d'
    ),
    CONCAT('user', (batch.b * 10000) + seq.n + 1, '@example.com'),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(((batch.b * 10000) + seq.n) * 11 + 2) * 730) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(((batch.b * 10000) + seq.n) * 11 + 3) * 365) DAY),
    NULL
FROM seq_helper seq
CROSS JOIN (
    SELECT 0 AS b UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
) batch
WHERE seq.n < 10000;
COMMIT;

SELECT COUNT(*) AS user_count FROM users;

-- =============================================================
-- 4. Products 생성 (1,000,000건)
-- =============================================================
DELETE FROM products;
ALTER TABLE products AUTO_INCREMENT = 1;

-- 1,000,000건 삽입 (100 배치 x 10,000건)
-- brand_id: 1~50,000 균등 분배
-- price: 10,000 ~ 500,000
-- stock_quantity: 1 ~ 500
-- like_count: 멱급수 분포 (대부분 낮고, 소수만 높음)
-- created_at: 최근 2년 내 분산
-- deleted_at: 5% soft deleted

INSERT INTO products (brand_id, name, description, price, stock_quantity, like_count, created_at, updated_at, deleted_at)
SELECT
    FLOOR(1 + RAND((@row_num := (batch.b * 10000) + seq.n) * 13 + 1) * 50000),
    CONCAT('Product_', @row_num + 1),
    CONCAT('Description for product ', @row_num + 1),
    FLOOR(10000 + RAND(@row_num * 13 + 2) * 490000),
    FLOOR(1 + RAND(@row_num * 13 + 3) * 500),
    FLOOR(POW(RAND(@row_num * 13 + 4), 3) * 10000),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(@row_num * 13 + 5) * 730) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(@row_num * 13 + 6) * 365) DAY),
    CASE
        WHEN RAND(@row_num * 13 + 7) < 0.05
        THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND(@row_num * 13 + 8) * 180) DAY)
        ELSE NULL
    END
FROM seq_helper seq
CROSS JOIN (
    SELECT 0 AS b UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34
    UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39
    UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44
    UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49
    UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
    UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59
    UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64
    UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69
    UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74
    UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79
    UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
    UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89
    UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94
    UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99
) batch
WHERE seq.n < 10000;
COMMIT;

-- =============================================================
-- 5. 정리
-- =============================================================
DROP TABLE IF EXISTS seq_helper;

-- 설정 복원
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET sql_mode = @saved_sql_mode;

-- =============================================================
-- 6. 검증
-- =============================================================
SELECT 'brands' AS table_name, COUNT(*) AS row_count FROM brands
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'products (active)', COUNT(*) FROM products WHERE deleted_at IS NULL
UNION ALL
SELECT 'products (deleted)', COUNT(*) FROM products WHERE deleted_at IS NOT NULL;
