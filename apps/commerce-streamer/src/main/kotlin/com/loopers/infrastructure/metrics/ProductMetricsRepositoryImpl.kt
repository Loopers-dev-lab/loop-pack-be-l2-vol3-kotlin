package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Repository

@Repository
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetrics? {
        return productMetricsJpaRepository.findById(productId).orElse(null)
    }

    override fun incrementLikeCount(productId: Long, version: Long) {
        productMetricsJpaRepository.incrementLikeCount(productId, version)
    }

    override fun decrementLikeCount(productId: Long, version: Long) {
        productMetricsJpaRepository.decrementLikeCount(productId, version)
    }

    override fun incrementSalesCount(productId: Long, quantity: Int) {
        productMetricsJpaRepository.incrementSalesCount(productId, quantity)
    }

    override fun incrementViewCount(productId: Long, version: Long) {
        productMetricsJpaRepository.incrementViewCount(productId, version)
    }

    override fun getVersion(productId: Long): Long? {
        return productMetricsJpaRepository.findById(productId).orElse(null)?.version
    }
}
