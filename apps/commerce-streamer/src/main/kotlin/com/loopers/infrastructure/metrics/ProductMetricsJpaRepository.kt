package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductMetricsJpaRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetrics?
    fun findByProductIdInAndMetricDate(productIds: Set<Long>, metricDate: LocalDate): List<ProductMetrics>
}
