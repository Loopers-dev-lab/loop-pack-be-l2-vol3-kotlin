package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaTopics
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.infrastructure.ranking.RankingEventType
import com.loopers.infrastructure.ranking.RankingScoreUpdater
import com.loopers.interfaces.consumer.fixture.ConsumerTestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate

class OrderEventConsumerTest {

    private lateinit var idempotencyService: IdempotencyService
    private lateinit var productMetricsJpaRepository: ProductMetricsJpaRepository
    private lateinit var rankingScoreUpdater: RankingScoreUpdater
    private lateinit var acknowledgment: Acknowledgment
    private lateinit var consumer: OrderEventConsumer

    @BeforeEach
    fun setUp() {
        idempotencyService = ConsumerTestFixtures.passThroughIdempotencyService()
        productMetricsJpaRepository = mockk(relaxed = true)
        rankingScoreUpdater = mockk(relaxed = true)
        acknowledgment = mockk(relaxUnitFun = true)
        consumer = OrderEventConsumer(idempotencyService, productMetricsJpaRepository, rankingScoreUpdater)
    }

    @Test
    fun `ORDER_COMPLETED 이벤트는 아이템별로 주문 수량과 매출을 반영한다`() {
        val record = createOrderRecord(
            eventId = "evt-1",
            eventType = "ORDER_COMPLETED",
            items = listOf(
                createOrderItem(productId = 10L, quantity = 2, salesAmount = 20_000L),
                createOrderItem(productId = 20L, quantity = 1, salesAmount = 5_000L),
            ),
        )

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify {
            productMetricsJpaRepository.upsertMetrics(
                productId = 10L,
                bucketDate = EVENT_DATE,
                viewCount = 0,
                likeCount = 0,
                orderCount = 2,
                salesAmount = 20_000L,
            )
        }
        verify {
            productMetricsJpaRepository.upsertMetrics(
                productId = 20L,
                bucketDate = EVENT_DATE,
                viewCount = 0,
                likeCount = 0,
                orderCount = 1,
                salesAmount = 5_000L,
            )
        }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `ORDER_COMPLETED 이벤트는 아이템 수량을 점수로 ORDER 가중치 랭킹에 반영한다`() {
        val record = createOrderRecord(
            eventId = "evt-2",
            eventType = "ORDER_COMPLETED",
            items = listOf(createOrderItem(productId = 10L, quantity = 3, salesAmount = 30_000L)),
        )

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify { rankingScoreUpdater.incrementScore(10L, RankingEventType.ORDER, EVENT_DATE, 3.0) }
    }

    @Test
    fun `아이템이 비어있는 ORDER_COMPLETED 이벤트는 아무것도 반영하지 않는다`() {
        val record = createOrderRecord(eventId = "evt-3", eventType = "ORDER_COMPLETED", items = emptyList())

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify(exactly = 0) {
            productMetricsJpaRepository.upsertMetrics(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) { rankingScoreUpdater.incrementScore(any(), any(), any(), any()) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `ORDER_CREATED 이벤트는 metrics와 랭킹에 반영하지 않는다`() {
        val record = createOrderRecord(eventId = "evt-4", eventType = "ORDER_CREATED", items = emptyList())

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify(exactly = 0) {
            productMetricsJpaRepository.upsertMetrics(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) { rankingScoreUpdater.incrementScore(any(), any(), any(), any()) }
    }

    @Test
    fun `이미 처리된 이벤트는 반영하지 않는다`() {
        every { idempotencyService.executeWithIdempotency(any(), any(), any()) } returns false
        val record = createOrderRecord(
            eventId = "evt-dup",
            eventType = "ORDER_COMPLETED",
            items = listOf(createOrderItem(productId = 10L, quantity = 1, salesAmount = 10_000L)),
        )

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify(exactly = 0) {
            productMetricsJpaRepository.upsertMetrics(any(), any(), any(), any(), any(), any())
        }
        verify { acknowledgment.acknowledge() }
    }

    private fun createOrderItem(productId: Long, quantity: Int, salesAmount: Long): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "quantity" to quantity,
            "salesAmount" to salesAmount,
        )
    }

    private fun createOrderRecord(
        eventId: String,
        eventType: String,
        items: List<Map<String, Any?>>,
    ): ConsumerRecord<Any, Any> {
        val avro = ConsumerTestFixtures.avroSchemaProvider.toGenericRecord(
            KafkaTopics.ORDER_EVENTS,
            mapOf(
                "eventId" to eventId,
                "eventType" to eventType,
                "orderId" to 100L,
                "userId" to 1L,
                "totalAmount" to items.sumOf { it["salesAmount"] as Long },
                "itemCount" to items.size,
                "items" to items,
                "occurredAt" to OCCURRED_AT,
            ),
        )
        return ConsumerRecord(KafkaTopics.ORDER_EVENTS, 0, 0L, null, avro)
    }

    companion object {
        private const val OCCURRED_AT = "2026-07-01T10:00:00+09:00"
        private val EVENT_DATE: LocalDate = LocalDate.of(2026, 7, 1)
    }
}
