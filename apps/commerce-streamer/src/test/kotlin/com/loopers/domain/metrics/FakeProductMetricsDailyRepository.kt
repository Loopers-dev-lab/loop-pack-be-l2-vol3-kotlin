package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.metrics.repository.ProductMetricsDailyRepository
import java.time.LocalDate

class FakeProductMetricsDailyRepository : ProductMetricsDailyRepository {

    private val store = mutableMapOf<Pair<LocalDate, Long>, ProductMetricsDaily>()
    private var sequence = 1L

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
        store[daily.metricDate to daily.productId] = persisted
        return persisted
    }
}
