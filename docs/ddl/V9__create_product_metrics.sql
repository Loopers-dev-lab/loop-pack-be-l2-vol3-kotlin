CREATE TABLE product_metrics (
    product_id  BIGINT      NOT NULL PRIMARY KEY,
    view_count  BIGINT      NOT NULL DEFAULT 0,
    like_count  BIGINT      NOT NULL DEFAULT 0,
    order_count BIGINT      NOT NULL DEFAULT 0,
    version     BIGINT      NOT NULL DEFAULT 0,
    updated_at  DATETIME(6) NOT NULL
);
