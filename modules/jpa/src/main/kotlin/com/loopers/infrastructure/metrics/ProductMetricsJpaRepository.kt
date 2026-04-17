package com.loopers.infrastructure.metrics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsEntity, Long> {
    fun findByMetricDateAndProductId(metricDate: LocalDate, productId: Long): ProductMetricsEntity?
}
