-- ============================================================
-- 인덱스 전/후 EXPLAIN 비교 스크립트
-- ============================================================
-- 사용법:
--   1. seed-products.sql 실행 후
--   2. [인덱스 없는 상태] 이 파일의 Phase 1 실행 → 결과 기록
--   3. Phase 2 (인덱스 추가) 실행
--   4. Phase 3 (인덱스 있는 상태) 실행 → 결과 기록
--   5. 비교!
-- ============================================================

-- ============================================================
-- Phase 0: 카디널리티/선택도 분석 (인덱스 설계 근거)
-- ============================================================

SELECT '=== 카디널리티 분석 ===' AS phase;

SELECT
    COUNT(DISTINCT brand_id)   AS brand_cardinality,
    COUNT(DISTINCT like_count) AS like_cardinality,
    COUNT(DISTINCT price)      AS price_cardinality,
    COUNT(*)                   AS total
FROM products
WHERE deleted_at IS NULL;

SELECT '=== brand_id 선택도 (상위 10개) ===' AS phase;

SELECT
    brand_id,
    COUNT(*) AS cnt,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM products WHERE deleted_at IS NULL), 2) AS selectivity_pct
FROM products
WHERE deleted_at IS NULL
GROUP BY brand_id
ORDER BY cnt DESC
LIMIT 10;

-- ============================================================
-- Phase 1: 인덱스 없는 상태에서 EXPLAIN (베이스라인)
-- ============================================================

SELECT '=== Phase 1: 인덱스 없는 상태 ===' AS phase;

-- 혹시 인덱스가 있으면 제거
DROP INDEX idx_product_brand_id ON products;
DROP INDEX idx_product_created_at_id ON products;
DROP INDEX idx_product_like_count_id ON products;
DROP INDEX idx_product_price_id ON products;
DROP INDEX idx_product_brand_like ON products;

SELECT '--- Q1. 최신순 (기본 정렬) ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT 20;

SELECT '--- Q2. 인기순 ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY like_count DESC LIMIT 20;

SELECT '--- Q3. 가격순 ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY price ASC LIMIT 20;

SELECT '--- Q4. 브랜드 필터 + 인기순 (대형 브랜드) ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL AND brand_id = (SELECT MIN(id) FROM brands)
ORDER BY like_count DESC LIMIT 20;

SELECT '--- Q5. COUNT (페이지네이션 totalElements) ---' AS query;
EXPLAIN ANALYZE SELECT COUNT(*) FROM products WHERE deleted_at IS NULL;

SELECT '--- Q6. 브랜드 필터 COUNT (소형 브랜드) ---' AS query;
EXPLAIN ANALYZE SELECT COUNT(*) FROM products
WHERE deleted_at IS NULL AND brand_id = (SELECT MIN(id) + 30 FROM brands);

-- ============================================================
-- Phase 2: 인덱스 추가
-- ============================================================

SELECT '=== Phase 2: 인덱스 추가 ===' AS phase;

CREATE INDEX idx_product_brand_id ON products (brand_id);
CREATE INDEX idx_product_created_at_id ON products (created_at DESC, id DESC);
CREATE INDEX idx_product_like_count_id ON products (like_count DESC, id DESC);
CREATE INDEX idx_product_price_id ON products (price ASC, id ASC);
CREATE INDEX idx_product_brand_like ON products (brand_id, like_count DESC, id DESC);

SHOW INDEX FROM products;

-- ============================================================
-- Phase 3: 인덱스 있는 상태에서 EXPLAIN (개선 후)
-- ============================================================

SELECT '=== Phase 3: 인덱스 있는 상태 ===' AS phase;

SELECT '--- Q1. 최신순 (기본 정렬) ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT 20;

SELECT '--- Q2. 인기순 ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY like_count DESC LIMIT 20;

SELECT '--- Q3. 가격순 ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL ORDER BY price ASC LIMIT 20;

SELECT '--- Q4. 브랜드 필터 + 인기순 (대형 브랜드) ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL AND brand_id = (SELECT MIN(id) FROM brands)
ORDER BY like_count DESC LIMIT 20;

SELECT '--- Q5. COUNT (페이지네이션 totalElements) ---' AS query;
EXPLAIN ANALYZE SELECT COUNT(*) FROM products WHERE deleted_at IS NULL;

SELECT '--- Q6. 브랜드 필터 COUNT (소형 브랜드) ---' AS query;
EXPLAIN ANALYZE SELECT COUNT(*) FROM products
WHERE deleted_at IS NULL AND brand_id = (SELECT MIN(id) + 30 FROM brands);

-- ============================================================
-- Phase 4: 반정규화 효과 비교 (likeCount vs JOIN)
-- ============================================================

SELECT '=== Phase 4: 반정규화 효과 비교 ===' AS phase;

SELECT '--- 반정규화 후 (현재 쿼리) ---' AS query;
EXPLAIN ANALYZE SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY like_count DESC LIMIT 20;

SELECT '--- 반정규화 없었다면 (JOIN + GROUP BY) ---' AS query;
EXPLAIN ANALYZE
SELECT p.*, COUNT(l.id) AS like_cnt
FROM products p
LEFT JOIN likes l ON p.id = l.product_id
WHERE p.deleted_at IS NULL
GROUP BY p.id
ORDER BY like_cnt DESC
LIMIT 20;

SELECT '=== 완료! 위 결과를 비교표로 정리하세요 ===' AS done;
