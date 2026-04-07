package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Component

@Component
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetrics? {
        return productMetricsJpaRepository.findByProductId(productId)
    }

    override fun save(productMetrics: ProductMetrics): ProductMetrics {
        return productMetricsJpaRepository.save(productMetrics)
    }

    override fun incrementLikeCount(productId: Long, eventVersion: Long): Int {
        return productMetricsJpaRepository.incrementLikeCount(productId, eventVersion)
    }

    override fun decrementLikeCount(productId: Long, eventVersion: Long): Int {
        return productMetricsJpaRepository.decrementLikeCount(productId, eventVersion)
    }

    override fun incrementViewCount(productId: Long, eventVersion: Long): Int {
        return productMetricsJpaRepository.incrementViewCount(productId, eventVersion)
    }

    override fun incrementOrderCount(productId: Long, eventVersion: Long): Int {
        return productMetricsJpaRepository.incrementOrderCount(productId, eventVersion)
    }
}
