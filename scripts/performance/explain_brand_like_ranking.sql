SHOW INDEX FROM products;
SHOW INDEX FROM likes;

EXPLAIN ANALYZE
SELECT
    id,
    brand_id,
    name,
    price,
    like_count,
    status
FROM products
WHERE brand_id = 7
ORDER BY like_count DESC, id DESC;

EXPLAIN ANALYZE
SELECT
    id,
    brand_id,
    name,
    price,
    like_count,
    status
FROM products
ORDER BY like_count DESC, id DESC;

EXPLAIN ANALYZE
SELECT
    id,
    brand_id,
    name,
    price,
    like_count,
    status
FROM products
WHERE brand_id = 7
ORDER BY id DESC;

EXPLAIN ANALYZE
SELECT
    product_id,
    COUNT(*) AS like_count
FROM likes
GROUP BY product_id
ORDER BY like_count DESC
LIMIT 20;
