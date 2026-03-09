-- 성능 테스트용 데이터 생성 (Brand 100개, Product 100K개)

-- 외래키 체크 비활성화 (데이터 정리를 위해)
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 데이터 정리 (중복 에러 방지)
DELETE FROM product_likes;
DELETE FROM products;
DELETE FROM brands;
DELETE FROM users;

-- 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- MySQL 재귀 깊이 제한 설정 (30만개 생성 필요)
SET SESSION cte_max_recursion_depth = 2000000;

-- 1. Brand 100개 생성 (임시 테이블 사용)
CREATE TEMPORARY TABLE temp_brands AS
WITH RECURSIVE brands_gen AS (
  SELECT 1 AS id
  UNION ALL
  SELECT id + 1 FROM brands_gen WHERE id < 100
)
SELECT id, CONCAT('Brand_', id) AS name, CONCAT('Description for Brand ', id) AS description FROM brands_gen;

INSERT INTO brands (id, name, description, created_at, updated_at)
SELECT id, name, description, NOW(), NOW() FROM temp_brands;

DROP TEMPORARY TABLE temp_brands;

-- 2. Product 100K개 생성 (임시 테이블 사용)
CREATE TEMPORARY TABLE temp_products AS
WITH RECURSIVE products_gen AS (
  SELECT 1 AS id
  UNION ALL
  SELECT id + 1 FROM products_gen WHERE id < 100000
)
SELECT
  id,
  ((id - 1) MOD 100) + 1 AS brand_id,
  CONCAT('Product_', id) AS name,
  (1000 + ((id - 1) MOD 500000)) AS price,
  IF((id MOD 5) = 0, 'INACTIVE', 'ACTIVE') AS status,
  (id MOD 1000) AS like_count
FROM products_gen;

INSERT INTO products (id, brand_id, name, price, status, like_count, created_at, updated_at)
SELECT id, brand_id, name, price, status, like_count, NOW(), NOW() FROM temp_products;

DROP TEMPORARY TABLE temp_products;

-- 3. User 10만 개 생성 (ProductLike 테스트용)
CREATE TEMPORARY TABLE temp_users AS
WITH RECURSIVE users_gen AS (
  SELECT 1 AS id
  UNION ALL
  SELECT id + 1 FROM users_gen WHERE id < 100000
)
SELECT
  id,
  CONCAT('user', id, '@test.com') AS email,
  CONCAT('user', id) AS login_id,
  'hashed_password' AS password,
  CONCAT('User_', id) AS name,
  '19900101' AS birth_date
FROM users_gen;

INSERT INTO users (email, login_id, password, name, birth_date, created_at, updated_at)
SELECT email, login_id, password, name, birth_date, NOW(), NOW() FROM temp_users;

DROP TEMPORARY TABLE temp_users;

-- 4. ProductLike 10만 개 생성 (Product 1~10에 분산)
-- 외래키 체크 비활성화 (ProductLike INSERT를 위해)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE temp_product_likes AS
WITH RECURSIVE likes_gen AS (
  SELECT 1 AS id
  UNION ALL
  SELECT id + 1 FROM likes_gen WHERE id < 100000
)
SELECT
  id AS user_id,
  ((id - 1) MOD 10) + 1 AS product_id
FROM likes_gen;

INSERT INTO product_likes (user_id, product_id, created_at, updated_at)
SELECT user_id, product_id, NOW(), NOW() FROM temp_product_likes;

DROP TEMPORARY TABLE temp_product_likes;

-- 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;
