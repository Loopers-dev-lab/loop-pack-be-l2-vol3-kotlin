package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity

/**
 * ProductMetrics Fake (테스트용).
 *
 * 실제 RepositoryImpl과 동일한 로직으로 구현한다.
 */
class FakeProductMetricsRepository : ProductMetricsRepository {

    private val store = mutableListOf<ProductMetrics>()
    private var idSequence = 1L

    override fun findByProductId(productId: Long): ProductMetrics? {
        return store.find { it.productId == productId }
    }

    override fun findByProductIds(productIds: Set<Long>): List<ProductMetrics> {
        if (productIds.isEmpty()) return emptyList()
        return store.filter { it.productId in productIds }
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        if (metrics.id == 0L) {
            setEntityId(metrics, idSequence++)
        }
        val existing = store.indexOfFirst { it.productId == metrics.productId }
        if (existing >= 0) {
            store[existing] = metrics
        } else {
            store.add(metrics)
        }
        return metrics
    }

    fun clear() {
        store.clear()
        idSequence = 1L
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
