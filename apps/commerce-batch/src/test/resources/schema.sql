CREATE TABLE IF NOT EXISTS brand (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    image_url   VARCHAR(512) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT NOW(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT NOW(6),
    deleted_at  DATETIME(6)  NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id       BIGINT       NOT NULL,
    name           VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    price          BIGINT       NOT NULL,
    stock_quantity INT          NOT NULL,
    like_count     INT          NOT NULL DEFAULT 0,
    image_url      VARCHAR(512) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT NOW(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT NOW(6),
    deleted_at     DATETIME(6)  NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product_like (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
    updated_at DATETIME(6) NOT NULL DEFAULT NOW(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_like_member_product (member_id, product_id)
);
