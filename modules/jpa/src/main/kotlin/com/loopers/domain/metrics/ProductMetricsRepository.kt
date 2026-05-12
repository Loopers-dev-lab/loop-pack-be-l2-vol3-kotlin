package com.loopers.domain.metrics

import java.time.LocalDate
import java.time.ZonedDateTime

data class ProductMetricsDeltaCommand(
    val productId: Long,
    val metricsDate: LocalDate,
    val viewDelta: Long,
    val likeDelta: Long,
    val salesDelta: Long,
    val scoreDelta: Double,
    val occurredAt: ZonedDateTime,
    val recordedAt: ZonedDateTime,
)

interface ProductMetricsRepository {
    fun upsertDailyMetrics(command: ProductMetricsDeltaCommand)

    fun findByProductIdAndMetricsDate(productId: Long, metricsDate: LocalDate): ProductMetricsModel?
}
