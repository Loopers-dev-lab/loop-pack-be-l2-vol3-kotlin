package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsDailyModel
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 테스트 시드 및 DDL 생성을 위한 경량 Repository.
 *
 * 배치 운영 경로(Reader) 는 raw SQL 을 사용하므로 본 인터페이스에 쿼리 메서드를 추가하지 않는다.
 */
interface ProductMetricsDailyJpaRepository : JpaRepository<ProductMetricsDailyModel, Long>
