CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price BIGINT NOT NULL,
    stock_quantity INT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    image_url VARCHAR(512) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);
CREATE INDEX IF NOT EXISTS idx_product_brand_id ON product (brand_id);
CREATE INDEX IF NOT EXISTS idx_product_status_price ON product (status, price ASC, id DESC);
CREATE INDEX IF NOT EXISTS idx_product_status_like_count ON product (status, like_count DESC, id DESC);
