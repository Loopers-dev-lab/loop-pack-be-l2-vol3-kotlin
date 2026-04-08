package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metrics.MetricsService
import com.loopers.application.ranking.RankingUpdater
import com.loopers.domain.ranking.RankingEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

/**
 * S1 — Order consumer 멱등성 + Ordered 이벤트의 amount 계산 검증.
 *
 * - metrics false → ranking 전체 skip (item 단위로 새지 않음)
 * - 다중 item 주문 → 각 item 마다 별도 ZINCRBY (amount = price × quantity)
 */
@DisplayName("OrderEventConsumer — S1 idempotency contract")
class OrderEventConsumerUnitTest {

    private val metricsService: MetricsService = mockk()
    private val rankingUpdater: RankingUpdater = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()
    private val ack: Acknowledgment = mockk(relaxed = true)

    private val consumer = OrderEventConsumer(metricsService, rankingUpdater, objectMapper)

    @Test
    fun `metrics 가 true 면 각 item 마다 Ordered 이벤트가 ranking 에 반영된다`() {
        every { metricsService.handleOrderPlaced(eq("evt-1"), any()) } returns true

        consumer.consume(
            listOf(
                record(
                    eventId = "evt-1",
                    items = listOf(Triple(100L, 2, 5_000), Triple(200L, 1, 30_000)),
                ),
            ),
            ack,
        )

        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Ordered(100L, amount = 10_000L)) }
        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Ordered(200L, amount = 30_000L)) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `metrics 가 false 면 모든 item 의 ranking 갱신을 건너뛴다`() {
        every { metricsService.handleOrderPlaced(eq("evt-dup"), any()) } returns false

        consumer.consume(
            listOf(
                record(
                    eventId = "evt-dup",
                    items = listOf(Triple(100L, 2, 5_000), Triple(200L, 1, 30_000)),
                ),
            ),
            ack,
        )

        verify(exactly = 0) { rankingUpdater.applyEvent(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `Redis 갱신 중 예외가 나도 ack 는 정상 호출된다`() {
        every { metricsService.handleOrderPlaced(any(), any()) } returns true
        every { rankingUpdater.applyEvent(any()) } throws RuntimeException("redis down")

        consumer.consume(
            listOf(record("evt-2", listOf(Triple(100L, 1, 10_000)))),
            ack,
        )

        verify(exactly = 1) { ack.acknowledge() }
    }

    private fun record(
        eventId: String,
        items: List<Triple<Long, Int, Int>>,
    ): ConsumerRecord<String, ByteArray> {
        val itemsJson = items.joinToString(",", "[", "]") { (productId, qty, price) ->
            """{"productId":$productId,"quantity":$qty,"price":$price}"""
        }
        val payload = """{"items":$itemsJson}"""
        val envelope = """
            {
              "eventId":"$eventId",
              "eventType":"ORDER_PLACED",
              "payload":${objectMapper.writeValueAsString(payload)}
            }
        """.trimIndent()
        return ConsumerRecord("order-events", 0, 0L, eventId, envelope.toByteArray())
    }
}
