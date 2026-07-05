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

class CatalogEventConsumerTest {

    private lateinit var idempotencyService: IdempotencyService
    private lateinit var productMetricsJpaRepository: ProductMetricsJpaRepository
    private lateinit var rankingScoreUpdater: RankingScoreUpdater
    private lateinit var acknowledgment: Acknowledgment
    private lateinit var consumer: CatalogEventConsumer

    @BeforeEach
    fun setUp() {
        idempotencyService = ConsumerTestFixtures.passThroughIdempotencyService()
        productMetricsJpaRepository = mockk(relaxed = true)
        rankingScoreUpdater = mockk(relaxed = true)
        acknowledgment = mockk(relaxUnitFun = true)
        consumer = CatalogEventConsumer(idempotencyService, productMetricsJpaRepository, rankingScoreUpdater)
    }

    @Test
    fun `PRODUCT_VIEWED 이벤트는 조회수 1과 VIEW 가중치 점수를 반영한다`() {
        val record = createCatalogRecord(eventId = "evt-1", eventType = "PRODUCT_VIEWED", productId = 10L)

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify {
            productMetricsJpaRepository.upsertMetrics(
                productId = 10L,
                bucketDate = EVENT_DATE,
                viewCount = 1,
                likeCount = 0,
                orderCount = 0,
                salesAmount = 0,
            )
        }
        verify { rankingScoreUpdater.incrementScore(10L, RankingEventType.VIEW, EVENT_DATE, 1.0) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `PRODUCT_LIKED 이벤트는 좋아요 1과 LIKE 가중치 점수를 반영한다`() {
        val record = createCatalogRecord(eventId = "evt-2", eventType = "PRODUCT_LIKED", productId = 10L)

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify {
            productMetricsJpaRepository.upsertMetrics(
                productId = 10L,
                bucketDate = EVENT_DATE,
                viewCount = 0,
                likeCount = 1,
                orderCount = 0,
                salesAmount = 0,
            )
        }
        verify { rankingScoreUpdater.incrementScore(10L, RankingEventType.LIKE, EVENT_DATE, 1.0) }
    }

    @Test
    fun `PRODUCT_UNLIKED 이벤트는 좋아요를 차감하고 랭킹 점수는 반영하지 않는다`() {
        val record = createCatalogRecord(eventId = "evt-3", eventType = "PRODUCT_UNLIKED", productId = 10L)

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify {
            productMetricsJpaRepository.upsertMetrics(
                productId = 10L,
                bucketDate = EVENT_DATE,
                viewCount = 0,
                likeCount = -1,
                orderCount = 0,
                salesAmount = 0,
            )
        }
        verify(exactly = 0) { rankingScoreUpdater.incrementScore(any(), any(), any(), any()) }
    }

    @Test
    fun `이미 처리된 이벤트는 metrics와 랭킹에 반영하지 않는다`() {
        every { idempotencyService.executeWithIdempotency(any(), any(), any()) } returns false
        val record = createCatalogRecord(eventId = "evt-dup", eventType = "PRODUCT_VIEWED", productId = 10L)

        consumer.consumeBatch(listOf(record), acknowledgment)

        verify(exactly = 0) {
            productMetricsJpaRepository.upsertMetrics(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) { rankingScoreUpdater.incrementScore(any(), any(), any(), any()) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `한 레코드 처리가 실패해도 나머지 레코드는 처리되고 ack된다`() {
        every { idempotencyService.executeWithIdempotency("evt-fail", any(), any()) } throws RuntimeException("DB down")
        val failing = createCatalogRecord(eventId = "evt-fail", eventType = "PRODUCT_VIEWED", productId = 10L)
        val healthy = createCatalogRecord(eventId = "evt-ok", eventType = "PRODUCT_VIEWED", productId = 20L)

        consumer.consumeBatch(listOf(failing, healthy), acknowledgment)

        verify { rankingScoreUpdater.incrementScore(20L, RankingEventType.VIEW, EVENT_DATE, 1.0) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `GenericRecord가 아닌 메시지는 건너뛰고 ack한다`() {
        val invalid = ConsumerRecord<Any, Any>(KafkaTopics.CATALOG_EVENTS, 0, 0L, null, "not-avro")

        consumer.consumeBatch(listOf(invalid), acknowledgment)

        verify(exactly = 0) {
            productMetricsJpaRepository.upsertMetrics(any(), any(), any(), any(), any(), any())
        }
        verify { acknowledgment.acknowledge() }
    }

    private fun createCatalogRecord(eventId: String, eventType: String, productId: Long): ConsumerRecord<Any, Any> {
        val avro = ConsumerTestFixtures.avroSchemaProvider.toGenericRecord(
            KafkaTopics.CATALOG_EVENTS,
            mapOf(
                "eventId" to eventId,
                "eventType" to eventType,
                "productId" to productId,
                "userId" to null,
                "occurredAt" to OCCURRED_AT,
            ),
        )
        return ConsumerRecord(KafkaTopics.CATALOG_EVENTS, 0, 0L, null, avro)
    }

    companion object {
        private const val OCCURRED_AT = "2026-07-01T10:00:00+09:00"
        private val EVENT_DATE: LocalDate = LocalDate.of(2026, 7, 1)
    }
}
