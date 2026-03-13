CREATE TABLE IF NOT EXISTS coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    value BIGINT NOT NULL,
    min_order_amount BIGINT NULL,
    max_discount_amount BIGINT NULL,
    expiration_policy VARCHAR(30) NOT NULL,
    expired_at DATETIME NULL,
    valid_days INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS issued_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_template_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    expired_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_issued_coupon_member_id ON issued_coupon (member_id);
CREATE INDEX IF NOT EXISTS idx_issued_coupon_template_id ON issued_coupon (coupon_template_id);
