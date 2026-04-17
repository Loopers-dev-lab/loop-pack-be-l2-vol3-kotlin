-- commerce-batch는 product_metrics_daily Entity를 정의하지 않으므로
-- Hibernate가 테이블을 자동 생성하지 않는다. 배치 E2E 테스트에서
-- jdbcTemplate으로 daily 데이터를 삽입하려면 이 테이블이 먼저 존재해야 한다.
-- 운영 환경에서는 migration tool(flyway 등) 또는 수동 DDL 적용으로 관리한다.
--
-- schema drift 방지: 기존 테이블이 다른 shape로 남아있으면 잘못된 테스트가 통과할 수 있으므로
-- 매 @Sql 실행마다 DROP + CREATE로 재생성한다.
DROP TABLE IF EXISTS product_metrics_daily;
CREATE TABLE product_metrics_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    metric_date DATE NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    units_sold INT NOT NULL DEFAULT 0,
    sales_amount BIGINT NOT NULL DEFAULT 0,
    order_score DOUBLE NOT NULL DEFAULT 0.0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    UNIQUE INDEX idx_product_metrics_daily_product_id_metric_date (product_id, metric_date)
);
