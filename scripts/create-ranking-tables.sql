-- 랭킹 시스템 테이블 생성 DDL

CREATE TABLE IF NOT EXISTS ranking_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL COMMENT 'VIEW, LIKE, ORDER',
    score DOUBLE NOT NULL COMMENT '가중치 적용된 점수',
    raw_count BIGINT NOT NULL DEFAULT 1 COMMENT '원본 건수',
    event_id VARCHAR(64) NOT NULL COMMENT '멱등성 키',
    aggregated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '집계 완료 여부',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    INDEX idx_ranking_event_agg_created (aggregated, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ranking_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    ranking_date VARCHAR(8) NOT NULL COMMENT 'yyyyMMdd',
    total_score DOUBLE NOT NULL DEFAULT 0 COMMENT '누적 점수',
    event_count BIGINT NOT NULL DEFAULT 0 COMMENT '누적 이벤트 수',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    UNIQUE INDEX uk_ranking_metric_product_date (product_id, ranking_date),
    INDEX idx_ranking_metric_date (ranking_date),
    INDEX idx_ranking_metric_date_product_score (ranking_date, product_id, total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주간 랭킹 TOP 100 (배치 Job으로 집계)
CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    ranking_rank INT NOT NULL,
    total_score DOUBLE NOT NULL,
    period_date VARCHAR(8) NOT NULL COMMENT '해당 주 월요일 yyyyMMdd',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    UNIQUE INDEX uk_weekly_period_rank (period_date, ranking_rank),
    INDEX idx_weekly_period_date (period_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 월간 랭킹 TOP 100 (배치 Job으로 집계)
CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    ranking_rank INT NOT NULL,
    total_score DOUBLE NOT NULL,
    period_date VARCHAR(6) NOT NULL COMMENT 'yyyyMM',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    UNIQUE INDEX uk_monthly_period_rank (period_date, ranking_rank),
    INDEX idx_monthly_period_date (period_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
