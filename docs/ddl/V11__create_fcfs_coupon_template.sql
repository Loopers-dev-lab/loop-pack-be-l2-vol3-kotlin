CREATE TABLE fcfs_coupon_template (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         TEXT         NULL,
    discount_type       VARCHAR(20)  NOT NULL,
    discount_value      BIGINT       NOT NULL,
    min_order_amount    BIGINT       NULL,
    max_discount_amount BIGINT       NULL,
    total_quantity      INT          NOT NULL,
    issued_quantity     INT          NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    started_at          DATETIME(6)  NOT NULL,
    ended_at            DATETIME(6)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL
);
