#!/bin/bash
# Redis 시드 데이터 적재 (Docker 환경용)

PRODUCT_COUNT=${1:-500}
DATE=$(date +%Y%m%d)
HOUR=$(date +%H)
DAILY_KEY="ranking:all:${DATE}"
HOURLY_KEY="ranking:all:${DATE}:${HOUR}"

echo "=== 랭킹 시드 데이터 생성 ==="
echo "상품 수: ${PRODUCT_COUNT}"
echo "Daily key: ${DAILY_KEY}"
echo "Hourly key: ${HOURLY_KEY}"

# 기존 키 삭제
docker exec redis redis-cli DEL "${DAILY_KEY}" "${HOURLY_KEY}"

# Redis pipeline 명령 생성
PIPE_FILE="/tmp/ranking-seed-pipe.txt"
> "${PIPE_FILE}"

for i in $(seq 1 ${PRODUCT_COUNT}); do
    SCORE=$(awk "BEGIN{srand($i * 31); printf \"%.2f\", rand() * 100 + rand() * 50}")
    echo "ZADD ${DAILY_KEY} ${SCORE} ${i}" >> "${PIPE_FILE}"
    HOURLY_SCORE=$(awk "BEGIN{srand($i * 17); printf \"%.2f\", ${SCORE} * (0.3 + rand() * 0.4)}")
    echo "ZADD ${HOURLY_KEY} ${HOURLY_SCORE} ${i}" >> "${PIPE_FILE}"
done

echo "EXPIRE ${DAILY_KEY} 172800" >> "${PIPE_FILE}"
echo "EXPIRE ${HOURLY_KEY} 10800" >> "${PIPE_FILE}"

# Docker로 파이프 전달
cat "${PIPE_FILE}" | docker exec -i redis redis-cli --pipe

echo ""
echo "Daily ZSET count: $(docker exec redis redis-cli ZCARD "${DAILY_KEY}")"
echo "Hourly ZSET count: $(docker exec redis redis-cli ZCARD "${HOURLY_KEY}")"
echo "Top 5 daily:"
docker exec redis redis-cli ZREVRANGE "${DAILY_KEY}" 0 4 WITHSCORES

# DB 시드 (user_action_log)
echo ""
echo "=== DB user_action_log 시드 ==="

SQL_FILE="/tmp/ranking-seed.sql"
> "${SQL_FILE}"

ACTION_TYPES=("VIEW" "VIEW" "VIEW" "VIEW" "VIEW" "LIKE" "LIKE" "ORDER")
COUNT=0

for pid in $(seq 1 ${PRODUCT_COUNT}); do
    ACTION_COUNT=$(( (RANDOM % 26) + 5 ))
    for j in $(seq 1 ${ACTION_COUNT}); do
        MID=$(( (RANDOM % 1000) + 1 ))
        AIDX=$(( RANDOM % 8 ))
        ATYPE=${ACTION_TYPES[$AIDX]}
        HR=$(printf "%02d" $(( RANDOM % 24 )))
        MN=$(printf "%02d" $(( RANDOM % 60 )))
        Y=${DATE:0:4}
        M=${DATE:4:2}
        D=${DATE:6:2}
        echo "INSERT INTO user_action_log (member_id, action_type, target_type, target_id, created_at) VALUES (${MID}, '${ATYPE}', 'PRODUCT', ${pid}, '${Y}-${M}-${D} ${HR}:${MN}:00');" >> "${SQL_FILE}"
        COUNT=$((COUNT + 1))
    done
done

echo "Records to insert: ${COUNT}"
docker exec -i docker-mysql-1 mysql -u root -ppassword commerce < "${SQL_FILE}" 2>/dev/null
echo "DB insert done"

DB_COUNT=$(docker exec docker-mysql-1 mysql -u root -ppassword commerce -N -e "SELECT COUNT(*) FROM user_action_log WHERE DATE_FORMAT(created_at, '%Y%m%d') = '${DATE}'" 2>/dev/null)
echo "DB record count: ${DB_COUNT}"

rm -f "${PIPE_FILE}" "${SQL_FILE}"
echo ""
echo "=== 시드 완료 ==="
