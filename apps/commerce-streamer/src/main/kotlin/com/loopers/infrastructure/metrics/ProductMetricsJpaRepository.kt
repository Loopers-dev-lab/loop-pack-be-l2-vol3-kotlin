package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository

interface ProductMetricsJpaRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductId(productId: Long): ProductMetrics?
    fun findByProductIdIn(productIds: Set<Long>): List<ProductMetrics>
}
