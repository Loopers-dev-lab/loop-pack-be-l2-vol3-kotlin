-- =============================================================
-- 인덱스 테스트용 대량 데이터 생성 스크립트
-- brands: 100개, products: 100,000개
-- 각 컬럼 값이 다양하게 분포하도록 설계
-- =============================================================

-- 기존 데이터 정리 (필요 시 주석 해제)
-- DELETE FROM products;
-- DELETE FROM brands;

-- ---------------------------------------------------------
-- 1. Brands 데이터 (100개)
-- ---------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_brands;

DELIMITER $$
CREATE PROCEDURE seed_brands()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE brand_name VARCHAR(255);
    DECLARE brand_desc VARCHAR(255);

    DECLARE categories JSON DEFAULT JSON_ARRAY(
        '패션', '전자기기', '식품', '가구', '뷰티',
        '스포츠', '도서', '완구', '주방', '건강',
        '자동차', '음악', '반려동물', '문구', '아웃도어',
        '홈데코', '유아', '디지털', '공구', '원예'
    );

    WHILE i <= 100 DO
        SET brand_name = CONCAT(
            JSON_UNQUOTE(JSON_EXTRACT(categories, CONCAT('$[', (i - 1) % 20, ']'))),
            ' 브랜드 ',
            LPAD(i, 3, '0')
        );
        SET brand_desc = CASE
            WHEN i % 5 = 0 THEN NULL
            ELSE CONCAT('브랜드 ', i, '의 설명입니다. 카테고리: ',
                         JSON_UNQUOTE(JSON_EXTRACT(categories, CONCAT('$[', (i - 1) % 20, ']'))))
        END;

        INSERT INTO brands (name, description, created_at, updated_at, deleted_at)
        VALUES (
            brand_name,
            brand_desc,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
            NULL
        );

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_brands();
DROP PROCEDURE IF EXISTS seed_brands;

-- ---------------------------------------------------------
-- 2. Products 데이터 (100,000개)
--    - name: 20개 카테고리 × 다양한 접미사 조합
--    - description: 20%는 NULL, 나머지는 다양한 텍스트
--    - price: 1,000 ~ 1,000,000 (롱테일 분포)
--    - likes: 0 ~ 50,000 (대부분 낮고, 일부 높음)
--    - stock_quantity: 0 ~ 10,000 (정규 분포 유사)
--    - brand_id: 멱급수(power-law) 분포 (일부 브랜드에 집중)
-- ---------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_products;

DELIMITER $$
CREATE PROCEDURE seed_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total INT DEFAULT 100000;

    DECLARE v_name VARCHAR(255);
    DECLARE v_desc VARCHAR(500);
    DECLARE v_price BIGINT;
    DECLARE v_likes INT;
    DECLARE v_stock INT;
    DECLARE v_brand_id BIGINT;
    DECLARE v_created_at DATETIME(6);

    -- 브랜드 ID 범위 조회
    DECLARE v_min_brand_id BIGINT;
    DECLARE v_max_brand_id BIGINT;
    DECLARE v_brand_count INT;

    SELECT MIN(id), MAX(id), COUNT(*) INTO v_min_brand_id, v_max_brand_id, v_brand_count FROM brands;

    -- 카테고리/형용사/명사 배열 (다양한 상품명 조합용)
    -- 카테고리 20개
    SET @cats = '패션,전자기기,식품,가구,뷰티,스포츠,도서,완구,주방,건강,자동차,음악,반려동물,문구,아웃도어,홈데코,유아,디지털,공구,원예';
    -- 형용사 20개
    SET @adjs = '프리미엄,베이직,럭셔리,에코,슬림,클래식,모던,빈티지,스마트,미니,울트라,맥스,프로,라이트,내추럴,오가닉,하이엔드,심플,스페셜,리미티드';
    -- 명사 25개
    SET @nouns = '세트,팩,키트,컬렉션,에디션,시리즈,라인,번들,박스,패키지,플러스,원,맥스,고,터보,엘리트,스타,퓨어,젠,네오,듀오,트리오,솔로,앱스,코어';

    WHILE i <= total DO
        -- 배치 단위 트랜잭션
        IF (i - 1) % batch_size = 0 THEN
            START TRANSACTION;
        END IF;

        -- 상품명: 카테고리 + 형용사 + 명사 + 번호
        SET v_name = CONCAT(
            SUBSTRING_INDEX(SUBSTRING_INDEX(@cats, ',', 1 + ((i - 1) % 20)), ',', -1),
            ' ',
            SUBSTRING_INDEX(SUBSTRING_INDEX(@adjs, ',', 1 + (FLOOR(i / 20) % 20)), ',', -1),
            ' ',
            SUBSTRING_INDEX(SUBSTRING_INDEX(@nouns, ',', 1 + (FLOOR(i / 400) % 25)), ',', -1),
            ' #',
            i
        );

        -- 설명: 20%는 NULL
        SET v_desc = CASE
            WHEN i % 5 = 0 THEN NULL
            ELSE CONCAT(
                '이 상품은 ',
                SUBSTRING_INDEX(SUBSTRING_INDEX(@cats, ',', 1 + ((i - 1) % 20)), ',', -1),
                ' 카테고리의 ',
                SUBSTRING_INDEX(SUBSTRING_INDEX(@adjs, ',', 1 + (FLOOR(i / 20) % 20)), ',', -1),
                ' 제품입니다. 상품번호: ', i,
                '. 품질 등급: ', CHAR(65 + (i % 5)),
                '. 원산지: ', ELT(1 + (i % 7), '한국', '미국', '일본', '독일', '프랑스', '이탈리아', '중국')
            )
        END;

        -- 가격: 롱테일 분포 (대부분 저가, 일부 고가)
        -- FLOOR(EXP(RAND() * LN(1000000))) → 1 ~ 1,000,000 사이 로그 분포
        SET v_price = GREATEST(1000, FLOOR(EXP(RAND() * LN(1000000))));

        -- 좋아요: 대부분 낮고 일부 높음 (지수 분포)
        SET v_likes = FLOOR(EXP(RAND() * LN(50001))) - 1;
        SET v_likes = GREATEST(0, v_likes);

        -- 재고: 정규 분포 유사 (Box-Muller 근사)
        -- 평균 500, 표준편차 300
        SET v_stock = GREATEST(0, LEAST(10000,
            FLOOR(500 + 300 * (RAND() + RAND() + RAND() + RAND() - 2))
        ));

        -- 브랜드 ID: 멱급수 분포 (앞쪽 브랜드에 더 많은 상품)
        -- FLOOR(POW(RAND(), 2) * brand_count) → 앞쪽 ID에 집중
        SET v_brand_id = v_min_brand_id + FLOOR(POW(RAND(), 2) * v_brand_count);
        SET v_brand_id = LEAST(v_brand_id, v_max_brand_id);

        -- 생성일: 최근 2년 내 다양한 시점
        SET v_created_at = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY)
                         + INTERVAL FLOOR(RAND() * 86400) SECOND;

        INSERT INTO products (name, description, price, likes, stock_quantity, brand_id, created_at, updated_at, deleted_at)
        VALUES (
            v_name,
            v_desc,
            v_price,
            v_likes,
            v_stock,
            v_brand_id,
            v_created_at,
            v_created_at,
            CASE WHEN i % 50 = 0 THEN DATE_ADD(v_created_at, INTERVAL FLOOR(RAND() * 180) DAY) ELSE NULL END
        );

        -- 배치 커밋
        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;

        SET i = i + 1;
    END WHILE;

    -- 마지막 배치 커밋
    IF (total % batch_size) != 0 THEN
        COMMIT;
    END IF;
END$$
DELIMITER ;

CALL seed_products();
DROP PROCEDURE IF EXISTS seed_products;

-- ---------------------------------------------------------
-- 3. 데이터 분포 확인 쿼리
-- ---------------------------------------------------------

-- 전체 건수 확인
SELECT 'brands' AS table_name, COUNT(*) AS cnt FROM brands
UNION ALL
SELECT 'products', COUNT(*) FROM products;

-- 브랜드별 상품 수 분포 (상위 10개, 하위 10개)
SELECT '== 브랜드별 상품 수 (상위 10) ==' AS info;
SELECT b.id, b.name, COUNT(p.id) AS product_count
FROM brands b LEFT JOIN products p ON b.id = p.brand_id
GROUP BY b.id, b.name
ORDER BY product_count DESC
LIMIT 10;

SELECT '== 브랜드별 상품 수 (하위 10) ==' AS info;
SELECT b.id, b.name, COUNT(p.id) AS product_count
FROM brands b LEFT JOIN products p ON b.id = p.brand_id
GROUP BY b.id, b.name
ORDER BY product_count ASC
LIMIT 10;

-- 가격 분포 (구간별)
SELECT '== 가격 분포 ==' AS info;
SELECT
    CASE
        WHEN price < 10000 THEN '1만 미만'
        WHEN price < 50000 THEN '1만~5만'
        WHEN price < 100000 THEN '5만~10만'
        WHEN price < 500000 THEN '10만~50만'
        ELSE '50만 이상'
    END AS price_range,
    COUNT(*) AS cnt,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM products), 1) AS pct
FROM products
GROUP BY price_range
ORDER BY MIN(price);

-- 좋아요 분포
SELECT '== 좋아요 분포 ==' AS info;
SELECT
    CASE
        WHEN likes < 10 THEN '10 미만'
        WHEN likes < 100 THEN '10~100'
        WHEN likes < 1000 THEN '100~1,000'
        WHEN likes < 10000 THEN '1,000~10,000'
        ELSE '10,000 이상'
    END AS likes_range,
    COUNT(*) AS cnt,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM products), 1) AS pct
FROM products
GROUP BY likes_range
ORDER BY MIN(likes);

-- 재고 분포
SELECT '== 재고 분포 ==' AS info;
SELECT
    MIN(stock_quantity) AS min_stock,
    MAX(stock_quantity) AS max_stock,
    ROUND(AVG(stock_quantity)) AS avg_stock,
    ROUND(STDDEV(stock_quantity)) AS stddev_stock
FROM products;

-- soft delete 비율
SELECT '== Soft Delete 비율 ==' AS info;
SELECT
    CASE WHEN deleted_at IS NULL THEN '활성' ELSE '삭제됨' END AS status,
    COUNT(*) AS cnt
FROM products
GROUP BY status;
