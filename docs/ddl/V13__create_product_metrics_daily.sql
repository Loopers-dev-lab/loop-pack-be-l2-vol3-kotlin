-- 일별 상품 메트릭 스냅샷 (Kafka Consumer가 실시간 UPSERT)
-- 주간/월간 랭킹 집계 Batch Job의 원천 테이블
CREATE TABLE product_metrics_daily (
    product_id        BIGINT      NOT NULL,
    metric_date       DATE        NOT NULL,
    view_count        BIGINT      NOT NULL DEFAULT 0,
    like_count        BIGINT      NOT NULL DEFAULT 0,
    order_count       BIGINT      NOT NULL DEFAULT 0,
    order_amount_sum  BIGINT      NOT NULL DEFAULT 0,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (product_id, metric_date),
    INDEX idx_product_metrics_daily_date (metric_date)
);

-- 주간 랭킹 Materialized View (Batch Job이 TOP 200만 적재)
-- period_key 포맷: ISO 8601 주차 "yyyy-Www" (예: "2026-W16")
CREATE TABLE mv_product_rank_weekly (
    period_key        VARCHAR(16) NOT NULL,
    product_id        BIGINT      NOT NULL,
    rank_value        INT         NOT NULL,
    score             DOUBLE      NOT NULL,
    view_count        BIGINT      NOT NULL DEFAULT 0,
    like_count        BIGINT      NOT NULL DEFAULT 0,
    order_count       BIGINT      NOT NULL DEFAULT 0,
    order_amount_sum  BIGINT      NOT NULL DEFAULT 0,
    computed_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (period_key, product_id),
    UNIQUE KEY uk_mv_product_rank_weekly_rank (period_key, rank_value),
    INDEX idx_mv_product_rank_weekly_rank (period_key, rank_value)
);

-- 월간 랭킹 Materialized View (Batch Job이 TOP 200만 적재)
-- period_key 포맷: "yyyyMM" (예: "202604")
CREATE TABLE mv_product_rank_monthly (
    period_key        VARCHAR(16) NOT NULL,
    product_id        BIGINT      NOT NULL,
    rank_value        INT         NOT NULL,
    score             DOUBLE      NOT NULL,
    view_count        BIGINT      NOT NULL DEFAULT 0,
    like_count        BIGINT      NOT NULL DEFAULT 0,
    order_count       BIGINT      NOT NULL DEFAULT 0,
    order_amount_sum  BIGINT      NOT NULL DEFAULT 0,
    computed_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (period_key, product_id),
    UNIQUE KEY uk_mv_product_rank_monthly_rank (period_key, rank_value),
    INDEX idx_mv_product_rank_monthly_rank (period_key, rank_value)
);

-- 전역 rank 부여용 중간 테이블 (Batch Job이 Step1에서 INSERT, Step2에서 SELECT+DELETE)
-- job_execution_id: Spring Batch BATCH_JOB_EXECUTION.JOB_EXECUTION_ID
CREATE TABLE rank_staging (
    job_execution_id  BIGINT      NOT NULL,
    product_id        BIGINT      NOT NULL,
    score             DOUBLE      NOT NULL,
    view_count        BIGINT      NOT NULL DEFAULT 0,
    like_count        BIGINT      NOT NULL DEFAULT 0,
    order_count       BIGINT      NOT NULL DEFAULT 0,
    order_amount_sum  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (job_execution_id, product_id),
    INDEX idx_rank_staging_score (job_execution_id, score DESC, product_id)
);
