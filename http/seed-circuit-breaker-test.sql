-- ============================================================
-- 서킷브레이커 테스트용 시드 데이터
-- 실행: docker exec -i docker-mysql-1 mysql -u application -papplication loopers < http/seed-circuit-breaker-test.sql
-- ============================================================

-- 1. 유저는 API로 회원가입 (POST /api/v1/users) — BCrypt 해시 호환 문제로 SQL 제외
-- .http 파일의 0-1 회원가입을 먼저 실행할 것

-- 2. 브랜드 1개
INSERT INTO brands (name, description, image_url, created_at, updated_at)
VALUES ('테스트브랜드', '서킷브레이커 테스트용', 'https://example.com/brand.png', NOW(), NOW());

-- 3. 상품 1개 (재고 충분히)
INSERT INTO products (brand_id, name, description, price, stock_quantity, like_count, status, display_yn, image_url, created_at, updated_at)
VALUES (1, '테스트상품', '서킷브레이커 테스트용 상품', 10000, 1000, 0, 'ACTIVE', true, 'https://example.com/product.png', NOW(), NOW());

-- 4. 주문 20건 (서킷 테스트용)
INSERT INTO orders (user_id, order_number, coupon_issue_id, original_total_amount, coupon_discount_amount, total_amount, order_status, created_at, updated_at)
VALUES
    (1, '26031800000001', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000002', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000003', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000004', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000005', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000006', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000007', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000008', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000009', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000010', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000011', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000012', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000013', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000014', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000015', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000016', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000017', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000018', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000019', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW()),
    (1, '26031800000020', NULL, 10000, 0, 10000, 'ORDERED', NOW(), NOW());

-- 5. 주문 아이템 20건
INSERT INTO order_items (order_id, product_id, quantity, item_status, snapshot_product_name, snapshot_brand_name, snapshot_brand_id, snapshot_image_url, snapshot_original_price, snapshot_discount_amount, snapshot_final_price, created_at, updated_at)
VALUES
    (1, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (2, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (3, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (4, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (5, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (6, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (7, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (8, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (9, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (10, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (11, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (12, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (13, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (14, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (15, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (16, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (17, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (18, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (19, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW()),
    (20, 1, 1, 'ORDERED', '테스트상품', '테스트브랜드', 1, 'https://example.com/product.png', 10000, 0, 10000, NOW(), NOW());

-- 확인
SELECT 'users' AS tbl, COUNT(*) AS cnt FROM users
UNION ALL SELECT 'brands', COUNT(*) FROM brands
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items;
