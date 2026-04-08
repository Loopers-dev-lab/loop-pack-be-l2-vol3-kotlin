package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Repository

@Repository
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {
    override fun findByProductId(productId: Long): ProductMetrics? {
        return productMetricsJpaRepository.findByProductId(productId)
    }

    override fun save(productMetrics: ProductMetrics): ProductMetrics {
        return productMetricsJpaRepository.save(productMetrics)
    }
}
