package com.loopers.application.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.metrics.EventHandledModel
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.metrics.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.ZonedDateTime

@DisplayName("ProductMetricsEventHandler - 랭킹 ZSET 연동")
class ProductMetricsEventHandlerRankingTest {
    private val eventHandledJpaRepository: EventHandledJpaRepository = mockk()
    private val productMetricsJpaRepository: ProductMetricsJpaRepository = mockk()
    private val rankingRepository: RankingRepository = mockk(relaxed = true)
    private val rankingScorePolicy = RankingScorePolicy(
        viewWeight = 0.1,
        likeWeight = 0.2,
        orderWeight = 0.7,
    )
    private val handler = ProductMetricsEventHandler(
        eventHandledJpaRepository = eventHandledJpaRepository,
        productMetricsJpaRepository = productMetricsJpaRepository,
        rankingRepository = rankingRepository,
        rankingScorePolicy = rankingScorePolicy,
    )

    @DisplayName("이벤트 처리 시 ZSET에 가중치 점수가 반영된다")
    @Test
    fun incrementsRankingScore() {
        // arrange
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val event = catalogEvent(
            eventId = "event-1",
            productId = 10L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )

        // act
        handler.handle(event)

        // assert
        verify(exactly = 1) {
            rankingRepository.incrementScore("ranking:all:20260410", 10L, 0.2)
        }
    }

    @DisplayName("주문 이벤트는 0.7의 가중치로 ZSET에 반영된다")
    @Test
    fun orderEventUsesOrderWeight() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val event = catalogEvent(
            eventId = "event-2",
            productId = 20L,
            eventType = CatalogEventType.ORDER_COMPLETED,
            delta = 2,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T15:00:00+09:00"),
        )

        handler.handle(event)

        verify(exactly = 1) {
            rankingRepository.incrementScore("ranking:all:20260410", 20L, 1.4)
        }
    }

    @DisplayName("중복 이벤트는 ZSET 점수를 갱신하지 않는다")
    @Test
    fun duplicateEventDoesNotIncrementScore() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val event = catalogEvent(
            eventId = "dup-event",
            productId = 10L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )

        handler.handle(event)
        handler.handle(event)

        verify(exactly = 1) {
            rankingRepository.incrementScore(any(), any(), any())
        }
    }

    @DisplayName("stale 이벤트는 metrics도 ZSET도 갱신하지 않는다")
    @Test
    fun staleEventDoesNotIncrementScore() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val latestEvent = catalogEvent(
            eventId = "latest",
            productId = 30L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 10,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )
        val staleEvent = catalogEvent(
            eventId = "stale",
            productId = 30L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 5,
            occurredAt = ZonedDateTime.parse("2026-04-10T11:00:00+09:00"),
        )

        handler.handle(latestEvent)
        handler.handle(staleEvent)

        // latest는 1회, stale는 0회
        verify(exactly = 1) {
            rankingRepository.incrementScore(any(), any(), any())
        }
    }

    private fun stubRepositories(
        handledEventIds: MutableSet<String>,
        metricsStore: MutableMap<Long, ProductMetricsModel>,
    ) {
        every { eventHandledJpaRepository.save(any<EventHandledModel>()) } answers {
            val eventHandled = firstArg<EventHandledModel>()
            if (!handledEventIds.add(eventHandled.eventId)) {
                throw DataIntegrityViolationException("duplicate event")
            }
            eventHandled
        }
        every { productMetricsJpaRepository.findByProductId(any()) } answers {
            metricsStore[firstArg()]
        }
        every { productMetricsJpaRepository.save(any<ProductMetricsModel>()) } answers {
            val metrics = firstArg<ProductMetricsModel>()
            metricsStore[metrics.productId] = metrics
            metrics
        }
    }

    private fun catalogEvent(
        eventId: String,
        productId: Long,
        eventType: CatalogEventType,
        delta: Long,
        version: Long,
        occurredAt: ZonedDateTime,
    ): CatalogEventMessage {
        return CatalogEventMessage(
            eventId = eventId,
            productId = productId,
            eventType = eventType,
            delta = delta,
            version = version,
            occurredAt = occurredAt,
        )
    }
}
