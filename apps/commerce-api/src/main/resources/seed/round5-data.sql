DROP TABLE IF EXISTS seed_seq_3;
DROP TABLE IF EXISTS seed_seq_10;
DROP TABLE IF EXISTS seed_seq_20;
DROP TABLE IF EXISTS seed_seq_30;
DROP TABLE IF EXISTS seed_seq_100000;
DROP TABLE IF EXISTS seed_seq_10000;
DROP TABLE IF EXISTS seed_digits;

CREATE TABLE seed_digits (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_digits (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TABLE seed_seq_10000 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_10000 (n)
SELECT d0.n + (d1.n * 10) + (d2.n * 100) + (d3.n * 1000) + 1 AS n
FROM seed_digits d0
    CROSS JOIN seed_digits d1
    CROSS JOIN seed_digits d2
    CROSS JOIN seed_digits d3;

CREATE TABLE seed_seq_100000 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_100000 (n)
SELECT d0.n + (d1.n * 10) + (d2.n * 100) + (d3.n * 1000) + (d4.n * 10000) + 1 AS n
FROM seed_digits d0
    CROSS JOIN seed_digits d1
    CROSS JOIN seed_digits d2
    CROSS JOIN seed_digits d3
    CROSS JOIN seed_digits d4;

CREATE TABLE seed_seq_30 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_30 (n)
SELECT n
FROM seed_seq_10000
WHERE n <= 30;

CREATE TABLE seed_seq_20 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_20 (n)
SELECT n
FROM seed_seq_10000
WHERE n <= 20;

CREATE TABLE seed_seq_10 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_10 (n)
SELECT n
FROM seed_seq_10000
WHERE n <= 10;

CREATE TABLE seed_seq_3 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO seed_seq_3 (n)
SELECT n
FROM seed_seq_10000
WHERE n <= 3;

INSERT INTO users (
    id,
    login_id,
    password,
    name,
    birth_date,
    email,
    created_at,
    updated_at
)
SELECT n AS id,
       CONCAT('seeduser', LPAD(n, 5, '0')) AS login_id,
       '$2y$10$HxCCBg7pB52ACs9nFxXA7ejw44TND9GZZGg4iizV4J.vmf1przfJG' AS password,
       '홍길동' AS name,
       '1990-01-01' AS birth_date,
       CONCAT('seed.user', LPAD(n, 5, '0'), '@example.com') AS email,
       TIMESTAMP('2026-02-01 09:00:00') + INTERVAL (n - 1) SECOND AS created_at,
       TIMESTAMP('2026-02-01 09:00:00') + INTERVAL (n - 1) SECOND AS updated_at
FROM seed_seq_10000;

INSERT INTO brand (
    id,
    name,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT n AS id,
       CONCAT('BRAND', LPAD(n, 4, '0')) AS name,
       'ACTIVE' AS status,
       TIMESTAMP('2026-02-01 10:00:00') + INTERVAL (n - 1) MINUTE AS created_at,
       TIMESTAMP('2026-02-01 10:00:00') + INTERVAL (n - 1) MINUTE AS updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM seed_seq_30;

INSERT INTO brand (
    id,
    name,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT 30 + n AS id,
       CONCAT('INACTIVEBRAND', LPAD(n, 2, '0')) AS name,
       'INACTIVE' AS status,
       TIMESTAMP('2026-02-01 12:00:00') + INTERVAL (n - 1) MINUTE AS created_at,
       TIMESTAMP('2026-02-01 12:00:00') + INTERVAL (n - 1) MINUTE AS updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM seed_seq_3;

INSERT INTO product (
    id,
    name,
    regular_price,
    selling_price,
    brand_id,
    image_url,
    thumbnail_url,
    like_count,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT n AS id,
       CONCAT('PRODUCT', LPAD(n, 6, '0')) AS name,
       CAST(10000 + ((n - 1) % 40) * 500 AS DECIMAL(19, 2)) AS regular_price,
       CAST((10000 + ((n - 1) % 40) * 500) - (((n - 1) % 5) * 250) AS DECIMAL(19, 2)) AS selling_price,
       ((n - 1) % 30) + 1 AS brand_id,
       NULL AS image_url,
       NULL AS thumbnail_url,
       0 AS like_count,
       'ACTIVE' AS status,
       TIMESTAMP('2026-02-01 14:00:00')
           + INTERVAL ((n - 1) % 30) DAY
           + INTERVAL ((n - 1) % 86400) SECOND AS created_at,
       TIMESTAMP('2026-02-01 14:00:00')
           + INTERVAL ((n - 1) % 30) DAY
           + INTERVAL ((n - 1) % 86400) SECOND AS updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM seed_seq_100000;

INSERT INTO product (
    id,
    name,
    regular_price,
    selling_price,
    brand_id,
    image_url,
    thumbnail_url,
    like_count,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT 100000 + n AS id,
       CONCAT('INACTIVEPRODUCTA', LPAD(n, 5, '0')) AS name,
       CAST(12000 + ((n - 1) % 20) * 700 AS DECIMAL(19, 2)) AS regular_price,
       CAST((12000 + ((n - 1) % 20) * 700) - (((n - 1) % 4) * 300) AS DECIMAL(19, 2)) AS selling_price,
       ((n - 1) % 30) + 1 AS brand_id,
       NULL AS image_url,
       NULL AS thumbnail_url,
       0 AS like_count,
       'INACTIVE' AS status,
       TIMESTAMP('2026-02-20 09:00:00') + INTERVAL (n - 1) MINUTE AS created_at,
       TIMESTAMP('2026-02-20 09:00:00') + INTERVAL (n - 1) MINUTE AS updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM seed_seq_10000
WHERE n <= 5000;

INSERT INTO product (
    id,
    name,
    regular_price,
    selling_price,
    brand_id,
    image_url,
    thumbnail_url,
    like_count,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT 105000 + n AS id,
       CONCAT('INACTIVEPRODUCTB', LPAD(n, 5, '0')) AS name,
       CAST(12000 + ((n - 1) % 20) * 700 AS DECIMAL(19, 2)) AS regular_price,
       CAST((12000 + ((n - 1) % 20) * 700) - (((n - 1) % 4) * 300) AS DECIMAL(19, 2)) AS selling_price,
       ((n - 1) % 3) + 31 AS brand_id,
       NULL AS image_url,
       NULL AS thumbnail_url,
       0 AS like_count,
       'INACTIVE' AS status,
       TIMESTAMP('2026-02-25 09:00:00') + INTERVAL (n - 1) MINUTE AS created_at,
       TIMESTAMP('2026-02-25 09:00:00') + INTERVAL (n - 1) MINUTE AS updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM seed_seq_10000
WHERE n <= 5000;

INSERT INTO product_stock (
    id,
    product_id,
    quantity,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT id,
       id AS product_id,
       CASE
           WHEN status = 'ACTIVE' THEN 20 + (id % 181)
           ELSE 5 + (id % 36)
       END AS quantity,
       created_at,
       updated_at,
       'loopers.seed' AS created_by,
       'loopers.seed' AS updated_by
FROM product;

INSERT INTO product_like (
    id,
    user_id,
    product_id,
    created_at
)
SELECT ((u.n - 1) * 20) + s.n AS id,
       u.n AS user_id,
       MOD(((u.n - 1) * 389) + ((s.n - 1) * 271), 5000) + 1 AS product_id,
       TIMESTAMP('2026-03-01 12:00:00')
           + INTERVAL ((u.n - 1) % 14) DAY
           + INTERVAL (s.n - 1) MINUTE AS created_at
FROM seed_seq_10000 u
    CROSS JOIN seed_seq_20 s;

INSERT INTO product_like (
    id,
    user_id,
    product_id,
    created_at
)
SELECT 200000 + ((u.n - 1) * 10) + s.n AS id,
       u.n AS user_id,
       MOD(((u.n - 1) * 613) + ((s.n - 1) * 271), 95000) + 5001 AS product_id,
       TIMESTAMP('2026-03-10 12:00:00')
           + INTERVAL ((u.n - 1) % 14) DAY
           + INTERVAL (s.n - 1) MINUTE AS created_at
FROM seed_seq_10000 u
    CROSS JOIN seed_seq_10 s;

UPDATE product p
    LEFT JOIN (
        SELECT product_id,
               COUNT(*) AS like_count
        FROM product_like
        GROUP BY product_id
    ) pl ON pl.product_id = p.id
SET p.like_count = COALESCE(pl.like_count, 0);

INSERT INTO orders (
    id,
    user_id,
    idempotency_key,
    status,
    issued_coupon_id,
    discount_amount,
    created_at,
    updated_at
)
SELECT n AS id,
       CASE
           WHEN n <= 30000 THEN MOD((n - 1) * 37, 4000) + 1
           WHEN n <= 45000 THEN MOD((n - 1) * 73, 7000) + 1
           ELSE MOD((n - 1) * 131, 10000) + 1
       END AS user_id,
       CONCAT('seed-order-', LPAD(n, 6, '0')) AS idempotency_key,
       'CREATED' AS status,
       NULL AS issued_coupon_id,
       CAST(0 AS DECIMAL(19, 2)) AS discount_amount,
       CASE
           WHEN n <= 35000 THEN TIMESTAMP('2026-03-13 09:00:00')
               - INTERVAL MOD(n - 1, 7) DAY
               - INTERVAL MOD((n - 1) * 53, 1440) MINUTE
           WHEN n <= 47000 THEN TIMESTAMP('2026-03-06 09:00:00')
               - INTERVAL (7 + MOD(n - 35001, 24)) DAY
               - INTERVAL MOD((n - 1) * 29, 1440) MINUTE
           ELSE TIMESTAMP('2026-02-06 09:00:00')
               - INTERVAL (30 + MOD(n - 47001, 60)) DAY
               - INTERVAL MOD((n - 1) * 17, 1440) MINUTE
       END AS created_at,
       CASE
           WHEN n <= 35000 THEN TIMESTAMP('2026-03-13 09:00:00')
               - INTERVAL MOD(n - 1, 7) DAY
               - INTERVAL MOD((n - 1) * 53, 1440) MINUTE
           WHEN n <= 47000 THEN TIMESTAMP('2026-03-06 09:00:00')
               - INTERVAL (7 + MOD(n - 35001, 24)) DAY
               - INTERVAL MOD((n - 1) * 29, 1440) MINUTE
           ELSE TIMESTAMP('2026-02-06 09:00:00')
               - INTERVAL (30 + MOD(n - 47001, 60)) DAY
               - INTERVAL MOD((n - 1) * 17, 1440) MINUTE
       END AS updated_at
FROM seed_seq_100000
WHERE n <= 50000;

INSERT INTO order_item (
    id,
    order_id,
    product_id,
    product_name,
    brand_id,
    brand_name,
    regular_price,
    selling_price,
    thumbnail_url,
    quantity,
    created_at,
    updated_at
)
SELECT ((o.id - 1) * 3) + s.n AS id,
       o.id AS order_id,
       p.id AS product_id,
       p.name AS product_name,
       b.id AS brand_id,
       b.name AS brand_name,
       p.regular_price,
       p.selling_price,
       p.thumbnail_url,
       MOD(o.id + s.n, 5) + 1 AS quantity,
       o.created_at,
       o.updated_at
FROM orders o
    JOIN seed_seq_3 s
        ON s.n <= CASE
                      WHEN MOD(o.id, 10) < 7 THEN 1
                      WHEN MOD(o.id, 10) < 9 THEN 2
                      ELSE 3
                  END
    JOIN product p
        ON p.id = CASE
                      WHEN MOD(o.id + s.n, 10) < 6 THEN MOD(((o.id - 1) * 97) + ((s.n - 1) * 31), 3000) + 1
                      WHEN MOD(o.id + s.n, 10) < 9 THEN MOD(((o.id - 1) * 193) + ((s.n - 1) * 17), 17000) + 3001
                      ELSE MOD(((o.id - 1) * 389) + ((s.n - 1) * 43), 80000) + 20001
                  END
    JOIN brand b
        ON b.id = p.brand_id
WHERE o.id <= 50000;

DROP TABLE IF EXISTS seed_seq_3;
DROP TABLE IF EXISTS seed_seq_10;
DROP TABLE IF EXISTS seed_seq_20;
DROP TABLE IF EXISTS seed_seq_30;
DROP TABLE IF EXISTS seed_seq_100000;
DROP TABLE IF EXISTS seed_seq_10000;
DROP TABLE IF EXISTS seed_digits;
