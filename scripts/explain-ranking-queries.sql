-- =============================================================
-- 랭킹 집계 쿼리 EXPLAIN 분석
-- 전제: ranking-metric-simulation.sql 실행 완료 (~810만 건)
-- =============================================================

-- =============================================================
-- 1. 인덱스 확인
-- =============================================================
SHOW INDEX FROM ranking_metric;

-- =============================================================
-- 2. 데이터 현황
-- =============================================================
SELECT COUNT(*) as total_rows,
       COUNT(DISTINCT product_id) as distinct_products,
       COUNT(DISTINCT ranking_date) as distinct_dates,
       MIN(ranking_date) as min_date,
       MAX(ranking_date) as max_date
FROM ranking_metric;

-- =============================================================
-- 3. 주간 집계 쿼리 (7일)
-- =============================================================

-- 3-1. EXPLAIN (실행 계획)
EXPLAIN
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260406' AND '20260412'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- 3-2. EXPLAIN ANALYZE (실제 실행 시간 포함)
EXPLAIN ANALYZE
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260406' AND '20260412'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- =============================================================
-- 4. 월간 집계 쿼리 (30일)
-- =============================================================

-- 4-1. EXPLAIN
EXPLAIN
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260316' AND '20260415'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- 4-2. EXPLAIN ANALYZE
EXPLAIN ANALYZE
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260316' AND '20260415'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- =============================================================
-- 5. 커버링 인덱스 추가 (성능 개선)
-- =============================================================

-- 5-1. 인덱스 생성
-- ranking_date 선행 → 날짜 범위 필터를 index range scan으로 처리
-- product_id 포함 → GROUP BY 활용
-- total_score 포함 → 커버링 인덱스 (테이블 접근 없이 SUM 계산)
CREATE INDEX idx_ranking_metric_date_product_score
ON ranking_metric (ranking_date, product_id, total_score);

-- 5-2. 주간 쿼리 재분석 (인덱스 적용 후)
EXPLAIN
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260406' AND '20260412'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

EXPLAIN ANALYZE
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260406' AND '20260412'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- 5-3. 월간 쿼리 재분석 (인덱스 적용 후)
EXPLAIN
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260316' AND '20260415'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

EXPLAIN ANALYZE
SELECT rm.product_id, SUM(rm.total_score) as total_score
FROM ranking_metric rm
WHERE rm.ranking_date BETWEEN '20260316' AND '20260415'
GROUP BY rm.product_id
ORDER BY total_score DESC
LIMIT 100;

-- =============================================================
-- 6. 분석 결과 요약 (810만 건 기준)
-- =============================================================
--
-- | 쿼리    | 인덱스 전   | 인덱스 후  | 개선 배율 | 스캔 방식 변화                                |
-- |---------|-----------|-----------|----------|----------------------------------------------|
-- | 주간 7일 | 204초     | 3.1초     | 66배     | Full index scan 8.1M → Range scan 630K       |
-- | 월간 30일| 223초     | 14.6초    | 15배     | Full index scan 8.1M → Range scan 2.79M      |
--
-- 개선 핵심: idx_ranking_metric_date_product_score 커버링 인덱스
-- - ranking_date 선행 → WHERE 범위 필터를 index range scan으로 처리
-- - product_id + total_score 포함 → GROUP BY + SUM을 인덱스만으로 처리 (Using index)
-- - 테이블 데이터 접근 없음 (Covering index range scan)
--
-- 배치 Job 실행 시간 관점:
-- - 주간 3.1초, 월간 14.6초 → 피크 시간 외 실행 시 서비스 영향 없음
