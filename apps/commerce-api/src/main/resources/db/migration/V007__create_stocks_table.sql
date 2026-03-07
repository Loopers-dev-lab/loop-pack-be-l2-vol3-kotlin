-- V007__create_stocks_table.sql
-- Stock 테이블 생성 및 기존 Product stock 데이터 마이그레이션

-- 1. Stock 테이블 생성
CREATE TABLE stocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 기존 Product의 stock을 Stock으로 마이그레이션
INSERT INTO stocks (product_id, quantity, created_at, updated_at)
SELECT id, stock, created_at, updated_at
FROM products
WHERE deleted_at IS NULL;

-- 3. Product 테이블에서 stock 컬럼 제거
ALTER TABLE products DROP COLUMN stock;

-- 4. Product 테이블에서 version 컬럼 제거
ALTER TABLE products DROP COLUMN version;
