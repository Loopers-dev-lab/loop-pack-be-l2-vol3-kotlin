package com.loopers.domain.metrics

import com.loopers.domain.ranking.ProductRankingScorePolicy
import com.loopers.messaging.catalog.CatalogEventMessage
import com.loopers.messaging.catalog.CatalogEventType
import com.loopers.messaging.order.OrderEventMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class ProductMetricsService(
    private val productMetricsRepository: ProductMetricsRepository,
) {
    companion object {
        private val METRICS_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    @Transactional
    fun apply(event: CatalogEventMessage) {
        val recordedAt = ZonedDateTime.now(METRICS_ZONE_ID)
        productMetricsRepository.upsertDailyMetrics(
            ProductMetricsDeltaCommand(
                productId = event.productId,
                metricsDate = event.occurredAt.withZoneSameInstant(METRICS_ZONE_ID).toLocalDate(),
                viewDelta = if (event.eventType == CatalogEventType.PRODUCT_VIEWED) 1L else 0L,
                likeDelta = when (event.eventType) {
                    CatalogEventType.PRODUCT_LIKED -> 1L
                    CatalogEventType.PRODUCT_UNLIKED -> -1L
                    CatalogEventType.PRODUCT_VIEWED -> 0L
                },
                salesDelta = 0L,
                scoreDelta = ProductRankingScorePolicy.catalogScore(event),
                occurredAt = event.occurredAt,
                recordedAt = recordedAt,
            ),
        )
    }

    @Transactional
    fun applySales(event: OrderEventMessage) {
        val metricsDate = event.occurredAt.withZoneSameInstant(METRICS_ZONE_ID).toLocalDate()
        val recordedAt = ZonedDateTime.now(METRICS_ZONE_ID)
        val orderScores = ProductRankingScorePolicy.orderScores(event)
        val groupedItems = event.items.groupBy { it.productId }

        groupedItems.forEach { (productId, items) ->
            productMetricsRepository.upsertDailyMetrics(
                ProductMetricsDeltaCommand(
                    productId = productId,
                    metricsDate = metricsDate,
                    viewDelta = 0L,
                    likeDelta = 0L,
                    salesDelta = items.sumOf { it.quantity },
                    scoreDelta = orderScores.getValue(productId),
                    occurredAt = event.occurredAt,
                    recordedAt = recordedAt,
                ),
            )
        }
    }
}
