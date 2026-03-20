CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL,
    transaction_key VARCHAR(100) NULL,
    card_type       VARCHAR(20)  NOT NULL,
    card_no         VARCHAR(19)  NOT NULL,
    amount          BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    fail_reason     VARCHAR(500) NULL,
    requested_at    DATETIME(6)  NOT NULL,
    completed_at    DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,

    UNIQUE INDEX uk_payments_transaction_key (transaction_key),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status_requested_at (status, requested_at)
);
