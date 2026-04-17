package com.loopers.application.metrics

import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class ProductMetricsUpdater(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) {
    companion object {
        private val METRIC_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun increaseLikeCount(productId: Long, occurredAt: ZonedDateTime) {
        val metrics = load(productId, occurredAt)
        if (!isNewer(metrics.lastLikeEventAt, occurredAt)) {
            return
        }
        metrics.likeCount += 1
        metrics.lastLikeEventAt = occurredAt
        productMetricsJpaRepository.save(metrics)
    }

    fun decreaseLikeCount(productId: Long, occurredAt: ZonedDateTime) {
        val metrics = load(productId, occurredAt)
        if (!isNewer(metrics.lastLikeEventAt, occurredAt)) {
            return
        }
        metrics.likeCount = (metrics.likeCount - 1L).coerceAtLeast(0L)
        metrics.lastLikeEventAt = occurredAt
        productMetricsJpaRepository.save(metrics)
    }

    fun increaseViewCount(productId: Long, occurredAt: ZonedDateTime) {
        val metrics = load(productId, occurredAt)
        if (!isNewer(metrics.lastViewEventAt, occurredAt)) {
            return
        }
        metrics.viewCount += 1
        metrics.lastViewEventAt = occurredAt
        productMetricsJpaRepository.save(metrics)
    }

    fun increaseSalesCount(productId: Long, quantity: Long, occurredAt: ZonedDateTime) {
        val metrics = load(productId, occurredAt)
        if (!isNewer(metrics.lastSalesEventAt, occurredAt)) {
            return
        }
        metrics.salesCount += quantity
        metrics.lastSalesEventAt = occurredAt
        productMetricsJpaRepository.save(metrics)
    }

    private fun load(productId: Long, occurredAt: ZonedDateTime): ProductMetricsEntity {
        val metricDate = metricDateOf(occurredAt)
        return productMetricsJpaRepository.findByMetricDateAndProductId(metricDate, productId)
            ?: ProductMetricsEntity(
                metricDate = metricDate,
                productId = productId,
            )
    }

    private fun metricDateOf(occurredAt: ZonedDateTime): LocalDate {
        return occurredAt.withZoneSameInstant(METRIC_ZONE).toLocalDate()
    }

    private fun isNewer(lastOccurredAt: ZonedDateTime?, occurredAt: ZonedDateTime): Boolean {
        return lastOccurredAt == null || occurredAt.isAfter(lastOccurredAt)
    }
}
