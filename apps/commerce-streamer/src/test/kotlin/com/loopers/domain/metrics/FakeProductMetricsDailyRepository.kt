package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.metrics.repository.ProductMetricsDailyRepository
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class FakeProductMetricsDailyRepository : ProductMetricsDailyRepository {

    private val store = mutableMapOf<Pair<LocalDate, Long>, ProductMetricsDaily>()
    private var sequence = 1L

    /** 0보다 크면, 신규(id==0) save 호출 시 해당 횟수만큼 DataIntegrityViolationException을 던진다. */
    var conflictsRemaining: Int = 0

    /** true이면 예외 전 store에 seed하여 재시도 find가 성공하도록 한다 (recoverable). */
    var recoverableConflict: Boolean = true

    override fun findByDateAndProductId(metricDate: LocalDate, productId: Long): ProductMetricsDaily? {
        return store[metricDate to productId]
    }

    override fun save(daily: ProductMetricsDaily): ProductMetricsDaily {
        val id = if (daily.id != 0L) daily.id else sequence++
        val persisted = ProductMetricsDaily(
            id = id,
            productId = daily.productId,
            metricDate = daily.metricDate,
            viewCount = daily.viewCount,
            likeCount = daily.likeCount,
            salesCount = daily.salesCount,
        )
        if (daily.id == 0L && conflictsRemaining > 0) {
            conflictsRemaining--
            if (recoverableConflict) store[daily.metricDate to daily.productId] = persisted
            throw DataIntegrityViolationException("simulated unique constraint violation")
        }
        store[daily.metricDate to daily.productId] = persisted
        return persisted
    }
}
