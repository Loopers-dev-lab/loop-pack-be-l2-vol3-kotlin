package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import org.springframework.dao.DataIntegrityViolationException

class FakeProductMetricsRepository : ProductMetricsRepository {

    private val store = mutableMapOf<Long, ProductMetrics>()
    private var sequence = 1L

    /** 0보다 크면, 신규(id==0) save 호출 시 해당 횟수만큼 DataIntegrityViolationException을 던진다. */
    var conflictsRemaining: Int = 0

    /** true이면 예외 전 store에 seed하여 재시도 find가 성공하도록 한다 (recoverable). */
    var recoverableConflict: Boolean = true

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
        if (metrics.id == 0L && conflictsRemaining > 0) {
            conflictsRemaining--
            if (recoverableConflict) store[metrics.productId] = persisted
            throw DataIntegrityViolationException("simulated unique constraint violation")
        }
        store[metrics.productId] = persisted
        return persisted
    }
}
