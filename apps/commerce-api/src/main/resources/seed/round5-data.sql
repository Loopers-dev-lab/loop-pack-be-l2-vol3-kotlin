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

DROP TABLE IF EXISTS seed_seq_3;
DROP TABLE IF EXISTS seed_seq_10;
DROP TABLE IF EXISTS seed_seq_20;
DROP TABLE IF EXISTS seed_seq_30;
DROP TABLE IF EXISTS seed_seq_100000;
DROP TABLE IF EXISTS seed_seq_10000;
DROP TABLE IF EXISTS seed_digits;
