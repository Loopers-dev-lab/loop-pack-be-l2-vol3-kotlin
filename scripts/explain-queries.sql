-- =============================================================
-- 성능 측정용 EXPLAIN ANALYZE 쿼리 모음
-- 각 시나리오별로 인덱스 적용 전/후 실행하여 비교
-- =============================================================

-- ----- 시나리오 1: 전체 + LATEST 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 2: brandId 필터 + LATEST 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL AND brand_id = 123
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 3: 전체 + PRICE_ASC 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY price ASC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 4: brandId 필터 + PRICE_ASC 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL AND brand_id = 456
ORDER BY price ASC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 5: 전체 + LIKES_DESC 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY like_count DESC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 6: brandId 필터 + LIKES_DESC 정렬 -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL AND brand_id = 789
ORDER BY like_count DESC
LIMIT 20 OFFSET 0;

-- ----- 시나리오 7: COUNT (전체) -----
EXPLAIN ANALYZE
SELECT COUNT(*) FROM products
WHERE deleted_at IS NULL;

-- ----- 시나리오 8: COUNT (brandId 필터) -----
EXPLAIN ANALYZE
SELECT COUNT(*) FROM products
WHERE deleted_at IS NULL AND brand_id = 123;

-- ----- 시나리오 9: 딥 페이지네이션 (offset 20000) -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 20000;

-- ----- 시나리오 10: 상품 상세 조회 (PK) -----
EXPLAIN ANALYZE
SELECT * FROM products
WHERE id = 500000 AND deleted_at IS NULL;

-- ----- 시나리오 11: 브랜드 상세 조회 (PK) -----
EXPLAIN ANALYZE
SELECT * FROM brands
WHERE id = 25000 AND deleted_at IS NULL;
