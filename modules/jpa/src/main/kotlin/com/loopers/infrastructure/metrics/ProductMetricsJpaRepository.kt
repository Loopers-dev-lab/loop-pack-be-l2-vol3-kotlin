package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsModel
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsModel, Long> {
    fun findByProductIdAndMetricsDate(productId: Long, metricsDate: LocalDate): ProductMetricsModel?
}
