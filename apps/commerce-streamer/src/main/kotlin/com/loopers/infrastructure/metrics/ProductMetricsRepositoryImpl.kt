package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Component

@Component
class ProductMetricsRepositoryImpl(
    private val jpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetrics? {
        return jpaRepository.findByProductId(productId)
    }

    override fun findByProductIds(productIds: Set<Long>): List<ProductMetrics> {
        if (productIds.isEmpty()) return emptyList()
        return jpaRepository.findByProductIdIn(productIds)
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        return jpaRepository.save(metrics)
    }
}
