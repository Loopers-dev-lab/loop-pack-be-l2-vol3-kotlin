package com.loopers.domain.metrics

import com.loopers.messaging.catalog.CatalogEventMessage
import com.loopers.messaging.catalog.CatalogEventType
import com.loopers.messaging.order.OrderEventMessage
import com.loopers.messaging.order.OrderEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ln

class ProductMetricsServiceTest {
    private val repository: ProductMetricsRepository = mock()
    private val service = ProductMetricsService(repository)
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    @Test
    fun `상품 조회 이벤트를 반영하면 일별 view delta와 ranking score를 누적한다`() {
        val occurredAt = ZonedDateTime.of(2026, 4, 17, 9, 0, 0, 0, zoneId)

        service.apply(
            CatalogEventMessage(
                eventId = "evt-1",
                eventType = CatalogEventType.PRODUCT_VIEWED,
                productId = 1L,
                actorLoginId = null,
                occurredAt = occurredAt,
            ),
        )

        val commandCaptor = argumentCaptor<ProductMetricsDeltaCommand>()
        verify(repository).upsertDailyMetrics(commandCaptor.capture())
        assertThat(commandCaptor.firstValue.productId).isEqualTo(1L)
        assertThat(commandCaptor.firstValue.metricsDate).isEqualTo(LocalDate.of(2026, 4, 17))
        assertThat(commandCaptor.firstValue.viewDelta).isEqualTo(1L)
        assertThat(commandCaptor.firstValue.likeDelta).isEqualTo(0L)
        assertThat(commandCaptor.firstValue.salesDelta).isEqualTo(0L)
        assertThat(commandCaptor.firstValue.scoreDelta).isEqualTo(0.1)
    }

    @Test
    fun `상품 좋아요 취소 이벤트를 반영하면 음수 like delta와 score delta를 전달한다`() {
        service.apply(
            CatalogEventMessage(
                eventId = "evt-2",
                eventType = CatalogEventType.PRODUCT_UNLIKED,
                productId = 1L,
                actorLoginId = "testuser",
                occurredAt = ZonedDateTime.now(zoneId),
            ),
        )

        val commandCaptor = argumentCaptor<ProductMetricsDeltaCommand>()
        verify(repository).upsertDailyMetrics(commandCaptor.capture())
        assertThat(commandCaptor.firstValue.likeDelta).isEqualTo(-1L)
        assertThat(commandCaptor.firstValue.scoreDelta).isEqualTo(-0.2)
    }

    @Test
    fun `주문 결제 성공 이벤트를 반영하면 상품별 sales delta와 점수를 upsert 한다`() {
        val occurredAt = ZonedDateTime.of(2026, 4, 17, 12, 30, 0, 0, zoneId)

        service.applySales(
            OrderEventMessage(
                eventId = "order-evt-1",
                eventType = OrderEventType.PAYMENT_SUCCEEDED,
                orderId = 100L,
                paymentId = 10L,
                userId = 1L,
                occurredAt = occurredAt,
                items = listOf(
                    OrderEventMessage.OrderEventItem(productId = 1L, quantity = 2L, unitPrice = 10_000L),
                    OrderEventMessage.OrderEventItem(productId = 2L, quantity = 1L, unitPrice = 20_000L),
                ),
            ),
        )

        val commandCaptor = argumentCaptor<ProductMetricsDeltaCommand>()
        verify(repository, times(2)).upsertDailyMetrics(commandCaptor.capture())

        val first = commandCaptor.allValues.first { it.productId == 1L }
        val second = commandCaptor.allValues.first { it.productId == 2L }
        assertThat(first.metricsDate).isEqualTo(LocalDate.of(2026, 4, 17))
        assertThat(first.salesDelta).isEqualTo(2L)
        assertThat(first.scoreDelta).isEqualTo(0.6 * ln(1 + 20_000.0))
        assertThat(second.salesDelta).isEqualTo(1L)
        assertThat(second.scoreDelta).isEqualTo(0.6 * ln(1 + 20_000.0))
    }
}
