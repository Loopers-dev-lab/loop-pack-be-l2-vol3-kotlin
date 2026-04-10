#!/bin/bash
# 랭킹 벤치마크 테스트용 시드 데이터 생성
# 사용법: ./k6/ranking-seed.sh [상품수] [날짜]
# 예시: ./k6/ranking-seed.sh 500 20260410

PRODUCT_COUNT=${1:-500}
DATE=${2:-$(date +%Y%m%d)}
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASS=${MYSQL_PASS:-password}
MYSQL_DB=${MYSQL_DB:-commerce}

DAILY_KEY="ranking:all:${DATE}"
HOUR=$(date +%H)
HOURLY_KEY="ranking:all:${DATE}:${HOUR}"

echo "=== 랭킹 벤치마크 시드 데이터 생성 ==="
echo "상품 수: ${PRODUCT_COUNT}"
echo "날짜: ${DATE}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "MySQL: ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}"
echo ""

# ── 1. Redis ZSET 시드 ──
echo "[1/3] Redis ZSET 시드 데이터 생성..."

# 기존 키 삭제
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} DEL "${DAILY_KEY}" "${HOURLY_KEY}" > /dev/null

# 파이프라인으로 한번에 적재
REDIS_CMDS=""
for i in $(seq 1 ${PRODUCT_COUNT}); do
    # 랜덤 점수: VIEW(0.1) * rand + LIKE(0.2) * rand + ORDER(0.7*log10) * rand
    SCORE=$(awk "BEGIN{srand($i * 31); printf \"%.2f\", rand() * 100 + rand() * 50}")
    REDIS_CMDS="${REDIS_CMDS}ZADD ${DAILY_KEY} ${SCORE} ${i}\n"
    # 시간별 키에는 일간의 30~70% 점수
    HOURLY_SCORE=$(awk "BEGIN{printf \"%.2f\", ${SCORE} * (0.3 + rand() * 0.4)}")
    REDIS_CMDS="${REDIS_CMDS}ZADD ${HOURLY_KEY} ${HOURLY_SCORE} ${i}\n"
done

# TTL 설정
REDIS_CMDS="${REDIS_CMDS}EXPIRE ${DAILY_KEY} 172800\n"
REDIS_CMDS="${REDIS_CMDS}EXPIRE ${HOURLY_KEY} 10800\n"

echo -e "${REDIS_CMDS}" | redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} --pipe 2>&1 | tail -1

echo "  Daily key: ${DAILY_KEY} ($(redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ZCARD ${DAILY_KEY}) entries)"
echo "  Hourly key: ${HOURLY_KEY} ($(redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ZCARD ${HOURLY_KEY}) entries)"

# ── 2. DB user_action_log 시드 ──
echo ""
echo "[2/3] DB user_action_log 시드 데이터 생성..."

# 임시 SQL 파일 생성
SQL_FILE="/tmp/ranking-seed-${DATE}.sql"
cat > "${SQL_FILE}" << 'HEADER'
SET @batch_size = 1000;
SET @commit_count = 0;
START TRANSACTION;
HEADER

ACTION_TYPES=("VIEW" "VIEW" "VIEW" "VIEW" "VIEW" "LIKE" "LIKE" "ORDER")
RECORD_COUNT=0

for product_id in $(seq 1 ${PRODUCT_COUNT}); do
    # 상품당 5~30개 액션 로그
    ACTION_COUNT=$((RANDOM % 26 + 5))
    for j in $(seq 1 ${ACTION_COUNT}); do
        MEMBER_ID=$((RANDOM % 1000 + 1))
        ACTION_IDX=$((RANDOM % 8))
        ACTION_TYPE=${ACTION_TYPES[$ACTION_IDX]}
        HOUR_RAND=$((RANDOM % 24))
        MIN_RAND=$((RANDOM % 60))

        echo "INSERT INTO user_action_log (member_id, action_type, target_type, target_id, created_at) VALUES (${MEMBER_ID}, '${ACTION_TYPE}', 'PRODUCT', ${product_id}, '${DATE:0:4}-${DATE:4:2}-${DATE:6:2} ${HOUR_RAND}:${MIN_RAND}:00');" >> "${SQL_FILE}"

        RECORD_COUNT=$((RECORD_COUNT + 1))
        if [ $((RECORD_COUNT % 1000)) -eq 0 ]; then
            echo "COMMIT;" >> "${SQL_FILE}"
            echo "START TRANSACTION;" >> "${SQL_FILE}"
        fi
    done
done

echo "COMMIT;" >> "${SQL_FILE}"

mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} < "${SQL_FILE}" 2>/dev/null
echo "  user_action_log: ${RECORD_COUNT} records inserted"

rm -f "${SQL_FILE}"

# ── 3. 검증 ──
echo ""
echo "[3/3] 시드 데이터 검증..."

REDIS_COUNT=$(redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ZCARD "${DAILY_KEY}")
echo "  Redis daily ZSET: ${REDIS_COUNT} entries"

REDIS_TOP3=$(redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ZREVRANGE "${DAILY_KEY}" 0 2 WITHSCORES)
echo "  Top 3: ${REDIS_TOP3}"

DB_COUNT=$(mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} -N -e "SELECT COUNT(*) FROM user_action_log WHERE DATE_FORMAT(created_at, '%Y%m%d') = '${DATE}'" 2>/dev/null)
echo "  DB action logs: ${DB_COUNT} records"

echo ""
echo "=== 시드 완료 ==="
