CREATE TABLE IF NOT EXISTS product_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_product_like_member_product ON product_like (member_id, product_id);
CREATE INDEX IF NOT EXISTS idx_product_like_product_id ON product_like (product_id);
