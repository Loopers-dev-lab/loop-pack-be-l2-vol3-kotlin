CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    order_number VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ORDERED',
    ordered_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_order_number ON orders (order_number);
CREATE INDEX IF NOT EXISTS idx_orders_member_ordered_at ON orders (member_id, ordered_at);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price BIGINT NOT NULL,
    brand_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item (order_id);
