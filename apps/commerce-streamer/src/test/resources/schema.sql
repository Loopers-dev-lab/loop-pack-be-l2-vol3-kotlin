CREATE TABLE IF NOT EXISTS kafka_consumed_event (
    event_id       VARCHAR(36)  NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    handled_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id, consumer_group)
);

CREATE TABLE IF NOT EXISTS fcfs_coupon_template (
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

CREATE TABLE IF NOT EXISTS fcfs_coupon_issue_request (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id   BIGINT      NOT NULL,
    member_id     BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6) NOT NULL,
    processed_at  DATETIME(6) NULL,
    INDEX idx_fcfs_req_template_member (template_id, member_id),
    INDEX idx_fcfs_req_status (status, created_at)
);

CREATE TABLE IF NOT EXISTS outbox_event (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    partition_key  VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    published_at   DATETIME(6)  NULL,
    INDEX idx_outbox_unpublished (published_at, created_at)
);

CREATE TABLE IF NOT EXISTS issued_coupon (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_template_id BIGINT      NOT NULL,
    member_id          BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    expired_at         DATETIME    NOT NULL,
    used_at            DATETIME    NULL,
    version            BIGINT      NOT NULL DEFAULT 0,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL
);
