package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metrics.MetricsService
import com.loopers.application.ranking.RankingUpdater
import com.loopers.domain.ranking.RankingEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

/**
 * S1 — Consumer 경계에서의 멱등성 검증.
 *
 * MetricsService.handleX 가 false (이미 처리된 eventId) 를 반환할 경우
 * RankingUpdater.applyEvent 도 함께 skip 되어야 한다 — 재처리 메시지로 인해
 * ZSET 점수가 중복 카운팅되면 안 되기 때문.
 */
@DisplayName("CatalogEventConsumer — S1 idempotency contract")
class CatalogEventConsumerUnitTest {

    private val metricsService: MetricsService = mockk()
    private val rankingUpdater: RankingUpdater = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()
    private val ack: Acknowledgment = mockk(relaxed = true)

    private val consumer = CatalogEventConsumer(metricsService, rankingUpdater, objectMapper)

    @Test
    fun `metrics 가 true 를 반환하면 ranking 도 함께 갱신된다`() {
        every { metricsService.handleProductViewed("evt-1", 100L) } returns true

        consumer.consume(listOf(record("evt-1", "PRODUCT_VIEWED", 100L)), ack)

        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Viewed(100L)) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `metrics 가 false 를 반환하면 ranking 은 skip 된다 (중복 카운트 방지)`() {
        every { metricsService.handleProductViewed("evt-dup", 100L) } returns false

        consumer.consume(listOf(record("evt-dup", "PRODUCT_VIEWED", 100L)), ack)

        verify(exactly = 0) { rankingUpdater.applyEvent(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `같은 eventId 가 두 번 들어오면 첫 번째만 ranking 에 반영된다`() {
        // 첫 호출 → 신규, 두 번째 호출 → 멱등 skip
        every { metricsService.handleProductLiked("evt-2", 200L) } returnsMany listOf(true, false)

        consumer.consume(
            listOf(
                record("evt-2", "PRODUCT_LIKED", 200L),
                record("evt-2", "PRODUCT_LIKED", 200L),
            ),
            ack,
        )

        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Liked(200L)) }
    }

    @Test
    fun `여러 이벤트 타입이 섞여 와도 각각 올바른 RankingEvent 로 매핑된다`() {
        every { metricsService.handleProductViewed("v", 100L) } returns true
        every { metricsService.handleProductLiked("l", 100L) } returns true
        every { metricsService.handleProductUnliked("u", 100L) } returns true

        consumer.consume(
            listOf(
                record("v", "PRODUCT_VIEWED", 100L),
                record("l", "PRODUCT_LIKED", 100L),
                record("u", "PRODUCT_UNLIKED", 100L),
            ),
            ack,
        )

        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Viewed(100L)) }
        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Liked(100L)) }
        verify(exactly = 1) { rankingUpdater.applyEvent(RankingEvent.Unliked(100L)) }
    }

    @Test
    fun `Redis 실패가 발생해도 ack 는 호출되어 다음 메시지로 진행한다 (Eventual Consistency)`() {
        every { metricsService.handleProductViewed("evt-3", 100L) } returns true
        every { rankingUpdater.applyEvent(any()) } throws RuntimeException("redis down")

        // 예외가 외부로 새지 않아야 한다 (try/catch + runCatching)
        consumer.consume(listOf(record("evt-3", "PRODUCT_VIEWED", 100L)), ack)

        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `알 수 없는 eventType 은 skip 되고 ranking 도 호출되지 않는다`() {
        consumer.consume(listOf(record("evt-x", "UNKNOWN_EVENT", 100L)), ack)

        verify(exactly = 0) { rankingUpdater.applyEvent(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    private fun record(eventId: String, eventType: String, productId: Long): ConsumerRecord<String, ByteArray> {
        val payload = """{"productId":$productId}"""
        val envelope = """
            {
              "eventId":"$eventId",
              "eventType":"$eventType",
              "payload":${objectMapper.writeValueAsString(payload)}
            }
        """.trimIndent()
        return ConsumerRecord("catalog-events", 0, 0L, eventId, envelope.toByteArray())
    }
}
