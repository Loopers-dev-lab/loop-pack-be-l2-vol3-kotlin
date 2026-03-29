-- streamer에 Coupon 엔티티가 없어서 Hibernate DDL이 coupons 테이블을 생성하지 않음.
-- 동시성 테스트에서 coupons 테이블이 필요하므로 직접 생성한다.
-- (프로덕션에서는 commerce-api가 DDL을 담당)
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    value BIGINT NOT NULL,
    expired_at DATETIME(6) NOT NULL,
    max_issue_count INT,
    issued_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
);
