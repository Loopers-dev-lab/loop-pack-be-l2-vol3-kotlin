package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository

class FakeProductMetricsRepository : ProductMetricsRepository {

    private val store = mutableMapOf<Long, ProductMetrics>()
    private var sequence = 1L

    override fun findByProductId(productId: Long): ProductMetrics? {
        return store[productId]
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        val id = if (metrics.id != 0L) metrics.id else sequence++
        val persisted = ProductMetrics(
            id = id,
            productId = metrics.productId,
            viewCount = metrics.viewCount,
            likeCount = metrics.likeCount,
            salesCount = metrics.salesCount,
        )
        store[metrics.productId] = persisted
        return persisted
    }
}
