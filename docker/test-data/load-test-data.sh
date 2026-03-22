#!/bin/bash

# 성능 테스트용 대량 데이터 생성 스크립트
# 사용: ./load-test-data.sh

set -e

# 설정
DB_HOST=${MYSQL_HOST:-localhost}
DB_PORT=${MYSQL_PORT:-3306}
DB_USER=${MYSQL_USER:-application}
DB_PASS=${MYSQL_PWD:-application}
DB_NAME="loopers"

echo "=========================================="
echo "테스트 데이터 생성 시작"
echo "=========================================="
echo "Host: $DB_HOST:$DB_PORT"
echo "Database: $DB_NAME"
echo ""

# 1단계: 브랜드 생성
echo "[1/5] 브랜드 데이터 생성 중... (100개)"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << 'EOF'
INSERT IGNORE INTO brands (name, description, created_at, updated_at, deleted_at)
SELECT
    CONCAT('Brand_', LPAD(n.id, 3, '0')) as name,
    CONCAT('Description for Brand ', n.id) as description,
    NOW() as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
    UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
    UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
    UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65
    UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70
    UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75
    UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80
    UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85
    UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
    UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95
    UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
) n;
EOF
BRAND_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -e "SELECT COUNT(*) FROM brands;")
echo "✓ 브랜드 생성 완료: $BRAND_COUNT개"
echo ""

# 2단계: 사용자 생성
echo "[2/5] 사용자 데이터 생성 중... (1000개)"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << 'EOFUSER'
INSERT IGNORE INTO users (login_id, password, name, birth_date, email, created_at, updated_at, deleted_at)
SELECT
    CONCAT('user_', LPAD(n.id, 5, '0')) as login_id,
    '$2a$10$8yw/Yq8qQwAqAYcqQwAqAYcqQwAqAYcqQwAqAYcqQwAqAYcqQwAqA' as password,
    CONCAT('User ', n.id) as name,
    DATE_SUB(CURDATE(), INTERVAL n.id DAY) as birth_date,
    CONCAT('user', n.id, '@example.com') as email,
    NOW() as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) base1,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) base2,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) base3,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) base4
LIMIT 1000;
EOFUSER
USER_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -e "SELECT COUNT(*) FROM users;")
echo "✓ 사용자 생성 완료: $USER_COUNT개"
echo ""

# 3단계: 상품 생성 (100만개)
echo "[3/5] 상품 데이터 생성 중... (1,000,000개)"
echo "      이 작업은 몇 분 걸릴 수 있습니다..."

START_TIME=$(date +%s)

# Bash 루프를 사용한 배치 삽입
BATCH_SIZE=5000
TOTAL=1000000

for ((i=1; i<=TOTAL; i+=BATCH_SIZE)); do
    END=$((i + BATCH_SIZE - 1))
    if [ $END -gt $TOTAL ]; then
        END=$TOTAL
    fi

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << EOF
INSERT IGNORE INTO products (brand_id, name, price, status, like_count, created_at, updated_at, deleted_at)
SELECT
    MOD(n.id - 1, 100) + 1 as brand_id,
    CONCAT('Product_', LPAD(n.id, 7, '0')) as name,
    ROUND(10000 + RAND() * 990000, 2) as price,
    IF(MOD(n.id, 5) = 0, 'INACTIVE', 'ACTIVE') as status,
    0 as like_count,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) t1,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t3,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t4,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t5
WHERE (t1.id - 1) * 100000 + (t2.id - 1) * 10000 + (t3.id - 1) * 1000 + (t4.id - 1) * 100 + (t5.id - 1) + 1 >= $i
AND (t1.id - 1) * 100000 + (t2.id - 1) * 10000 + (t3.id - 1) * 1000 + (t4.id - 1) * 100 + (t5.id - 1) + 1 <= $END;
EOF

    CURRENT_COUNT=$((i + BATCH_SIZE - 1))
    if [ $CURRENT_COUNT -gt $TOTAL ]; then
        CURRENT_COUNT=$TOTAL
    fi
    PERCENTAGE=$((CURRENT_COUNT * 100 / TOTAL))
    echo "      진행: $CURRENT_COUNT / $TOTAL ($PERCENTAGE%)"
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

PRODUCT_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -e "SELECT COUNT(*) FROM products WHERE deleted_at IS NULL;")
echo "✓ 상품 생성 완료: $PRODUCT_COUNT개 (소요 시간: ${ELAPSED}초)"
echo ""

# 4단계: 좋아요 생성 (100만개)
echo "[4/5] 좋아요 데이터 생성 중... (1,000,000개)"
echo "      이 작업은 몇 분 걸릴 수 있습니다..."

START_TIME=$(date +%s)

for ((i=1; i<=TOTAL; i+=BATCH_SIZE)); do
    END=$((i + BATCH_SIZE - 1))
    if [ $END -gt $TOTAL ]; then
        END=$TOTAL
    fi

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << EOF
INSERT IGNORE INTO product_likes (user_id, product_id, created_at, updated_at, deleted_at)
SELECT
    MOD(n.id - 1, 1000) + 1 as user_id,
    MOD(n.id - 1, 1000000) + 1 as product_id,
    NOW() as created_at,
    NOW() as updated_at,
    NULL as deleted_at
FROM (
    SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) t1,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t3,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t4,
(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t5
WHERE (t1.id - 1) * 100000 + (t2.id - 1) * 10000 + (t3.id - 1) * 1000 + (t4.id - 1) * 100 + (t5.id - 1) + 1 >= $i
AND (t1.id - 1) * 100000 + (t2.id - 1) * 10000 + (t3.id - 1) * 1000 + (t4.id - 1) * 100 + (t5.id - 1) + 1 <= $END;
EOF

    CURRENT_COUNT=$((i + BATCH_SIZE - 1))
    if [ $CURRENT_COUNT -gt $TOTAL ]; then
        CURRENT_COUNT=$TOTAL
    fi
    PERCENTAGE=$((CURRENT_COUNT * 100 / TOTAL))
    echo "      진행: $CURRENT_COUNT / $TOTAL ($PERCENTAGE%)"
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

LIKE_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -e "SELECT COUNT(*) FROM product_likes WHERE deleted_at IS NULL;")
echo "✓ 좋아요 생성 완료: $LIKE_COUNT개 (소요 시간: ${ELAPSED}초)"
echo ""

# 5단계: 좋아요 개수 업데이트
echo "[5/5] 상품 좋아요 개수 업데이트 중..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << 'EOF'
UPDATE products p
SET like_count = (
    SELECT COUNT(*)
    FROM product_likes pl
    WHERE pl.product_id = p.id
    AND pl.deleted_at IS NULL
)
WHERE p.deleted_at IS NULL;
EOF
echo "✓ 좋아요 개수 업데이트 완료"
echo ""

# 최종 통계
echo "=========================================="
echo "테스트 데이터 생성 완료!"
echo "=========================================="
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" << 'EOF'
SELECT
    CONCAT('브랜드: ', (SELECT COUNT(*) FROM brands)) as statistics1,
    CONCAT('사용자: ', (SELECT COUNT(*) FROM users)) as statistics2,
    CONCAT('상품: ', (SELECT COUNT(*) FROM products WHERE deleted_at IS NULL)) as statistics3,
    CONCAT('좋아요: ', (SELECT COUNT(*) FROM product_likes WHERE deleted_at IS NULL)) as statistics4;
EOF
