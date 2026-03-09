-- 외래키 체크 비활성화 (데이터 정리를 위해)
SET FOREIGN_KEY_CHECKS = 0;

-- 성능 테스트 데이터 정리
DELETE FROM product_likes;
DELETE FROM products;
DELETE FROM brands;
DELETE FROM users;

-- 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 자동 증가값 초기화
ALTER TABLE brands AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE product_likes AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
