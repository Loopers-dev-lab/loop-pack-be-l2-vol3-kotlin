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
        } else {
            setId(metrics, sequence++)
            store.add(metrics)
        }
        return metrics
    }

    private fun setId(entity: ProductMetrics, id: Long) {
        ProductMetrics::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
