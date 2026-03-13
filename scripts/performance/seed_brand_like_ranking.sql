SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE likes;
TRUNCATE TABLE products;
TRUNCATE TABLE brands;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO brands (name, status, created_at, updated_at)
WITH digits AS (
    SELECT 0 AS n
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
),
numbers AS (
    SELECT ones.n + (tens.n * 10) + 1 AS seq
    FROM digits ones
    CROSS JOIN digits tens
)
SELECT
    CONCAT('BRAND-', LPAD(seq, 3, '0')),
    'ACTIVE',
    NOW(),
    NOW()
FROM numbers
WHERE seq <= 100;

INSERT INTO products (brand_id, name, price, description, stock, like_count, status, created_at, updated_at)
WITH digits AS (
    SELECT 0 AS n
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
),
numbers AS (
    SELECT
        ones.n
        + (tens.n * 10)
        + (hundreds.n * 100)
        + (thousands.n * 1000)
        + (tenThousands.n * 10000)
        + 1 AS seq
    FROM digits ones
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
    CROSS JOIN digits thousands
    CROSS JOIN digits tenThousands
)
SELECT
    ((seq - 1) % 100) + 1 AS brand_id,
    CONCAT('PRODUCT-', LPAD(seq, 6, '0')) AS name,
    1000 + ((seq * 73) % 900000) AS price,
    CONCAT('Seeded product ', seq) AS description,
    ((seq * 17) % 100) + 1 AS stock,
    0 AS like_count,
    'SELLING' AS status,
    DATE_SUB(NOW(), INTERVAL (seq % 365) DAY) AS created_at,
    NOW() AS updated_at
FROM numbers
WHERE seq <= 100000;

INSERT INTO likes (member_id, product_id, created_at)
WITH digits AS (
    SELECT 0 AS n
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
),
products_seed AS (
    SELECT
        ones.n
        + (tens.n * 10)
        + (hundreds.n * 100)
        + (thousands.n * 1000)
        + (tenThousands.n * 10000)
        + 1 AS product_seq
    FROM digits ones
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
    CROSS JOIN digits thousands
    CROSS JOIN digits tenThousands
),
like_slots AS (
    SELECT ones.n + 1 AS slot
    FROM digits ones
    WHERE ones.n < 20
)
SELECT
    (product_seq * 100) + slot AS member_id,
    product_seq AS product_id,
    DATE_SUB(NOW(), INTERVAL ((product_seq + slot) % 30) DAY) AS created_at
FROM products_seed
JOIN like_slots
    ON like_slots.slot <= CASE
        WHEN product_seq % 1000 = 0 THEN 20
        WHEN product_seq % 100 = 0 THEN 10
        WHEN product_seq % 10 = 0 THEN 4
        ELSE 1
    END
WHERE product_seq <= 100000;

UPDATE products p
LEFT JOIN (
    SELECT product_id, COUNT(*) AS like_count
    FROM likes
    GROUP BY product_id
) l
    ON l.product_id = p.id
SET p.like_count = COALESCE(l.like_count, 0);

ANALYZE TABLE brands, products, likes;
