package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository

class FakeProductMetricsRepository : ProductMetricsRepository {

    private val store = mutableListOf<ProductMetrics>()
    private var sequence = 1L

    override fun findByProductId(productId: Long): ProductMetrics? {
        return store.find { it.productId == productId }
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        if (metrics.id != 0L) {
            store.removeIf { it.id == metrics.id }
            store.add(metrics)
            return metrics
        }
        val persisted = ProductMetrics(
            id = sequence++,
            productId = metrics.productId,
            viewCount = metrics.viewCount,
            likeCount = metrics.likeCount,
            salesCount = metrics.salesCount,
        )
        store.add(persisted)
        return persisted
    }
}
