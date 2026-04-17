package com.loopers.application.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.metrics.EventHandledModel
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.metrics.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.ZonedDateTime

@DisplayName("ProductMetricsEventHandler - product_metrics_daily 적재")
class ProductMetricsEventHandlerDailyTest {
    private val eventHandledJpaRepository: EventHandledJpaRepository = mockk()
    private val productMetricsJpaRepository: ProductMetricsJpaRepository = mockk()
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository = mockk(relaxed = true)
    private val rankingRepository: RankingRepository = mockk(relaxed = true)
    private val rankingScorePolicy = RankingScorePolicy(
        viewWeight = 0.1,
        likeWeight = 0.2,
        orderWeight = 0.7,
    )
    private val handler = ProductMetricsEventHandler(
        eventHandledJpaRepository = eventHandledJpaRepository,
        productMetricsJpaRepository = productMetricsJpaRepository,
        productMetricsDailyJpaRepository = productMetricsDailyJpaRepository,
        rankingRepository = rankingRepository,
        rankingScorePolicy = rankingScorePolicy,
    )

    private val handledEventIds = mutableSetOf<String>()
    private val metricsStore = mutableMapOf<Long, ProductMetricsModel>()

    @DisplayName("LIKE_CHANGED 이벤트는 likesDelta 만 채워 daily upsert 된다")
    @Test
    fun likeEventUpsertsDailyWithLikesDelta() {
        stubRepositories()
        val event = catalogEvent(
            eventId = "event-like",
            productId = 10L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )

        handler.handle(event)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 10L,
                metricDate = LocalDate.of(2026, 4, 10),
                likesDelta = 1,
                viewsDelta = 0,
                salesDelta = 0,
            )
        }
    }

    @DisplayName("PRODUCT_VIEWED 이벤트는 viewsDelta 만 채워 daily upsert 된다")
    @Test
    fun viewEventUpsertsDailyWithViewsDelta() {
        stubRepositories()
        val event = catalogEvent(
            eventId = "event-view",
            productId = 20L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T15:00:00+09:00"),
        )

        handler.handle(event)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 20L,
                metricDate = LocalDate.of(2026, 4, 10),
                likesDelta = 0,
                viewsDelta = 1,
                salesDelta = 0,
            )
        }
    }

    @DisplayName("ORDER_COMPLETED 이벤트는 salesDelta 만 delta 수량만큼 채워 upsert 된다")
    @Test
    fun orderEventUpsertsDailyWithSalesDelta() {
        stubRepositories()
        val event = catalogEvent(
            eventId = "event-order",
            productId = 30L,
            eventType = CatalogEventType.ORDER_COMPLETED,
            delta = 5,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )

        handler.handle(event)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 30L,
                metricDate = LocalDate.of(2026, 4, 10),
                likesDelta = 0,
                viewsDelta = 0,
                salesDelta = 5,
            )
        }
    }

    @DisplayName("같은 상품·같은 날짜 이벤트가 누적되어도 각 호출이 upsert 로 전달된다")
    @Test
    fun multipleEventsSameDayAllInvokeUpsert() {
        stubRepositories()
        val occurredAt = ZonedDateTime.parse("2026-04-10T09:00:00+09:00")
        val firstLike = catalogEvent("e-like-1", 40L, CatalogEventType.LIKE_CHANGED, 1, 1, occurredAt)
        val secondLike = catalogEvent("e-like-2", 40L, CatalogEventType.LIKE_CHANGED, 1, 2, occurredAt.plusHours(1))

        handler.handle(firstLike)
        handler.handle(secondLike)

        verify(exactly = 2) {
            productMetricsDailyJpaRepository.upsert(
                productId = 40L,
                metricDate = LocalDate.of(2026, 4, 10),
                likesDelta = 1,
                viewsDelta = 0,
                salesDelta = 0,
            )
        }
    }

    @DisplayName("같은 상품이어도 날짜가 다르면 각각 다른 metricDate 로 upsert 된다")
    @Test
    fun differentDatesTriggerDifferentMetricDates() {
        stubRepositories()
        val monday = catalogEvent(
            eventId = "e-monday",
            productId = 50L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-13T12:00:00+09:00"),
        )
        val tuesday = catalogEvent(
            eventId = "e-tuesday",
            productId = 50L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 2,
            occurredAt = ZonedDateTime.parse("2026-04-14T12:00:00+09:00"),
        )

        handler.handle(monday)
        handler.handle(tuesday)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 50L,
                metricDate = LocalDate.of(2026, 4, 13),
                likesDelta = 0,
                viewsDelta = 1,
                salesDelta = 0,
            )
        }
        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 50L,
                metricDate = LocalDate.of(2026, 4, 14),
                likesDelta = 0,
                viewsDelta = 1,
                salesDelta = 0,
            )
        }
    }

    @DisplayName("중복 eventId 는 daily upsert 에도 반영되지 않는다 (멱등 가드)")
    @Test
    fun duplicateEventDoesNotUpsertDaily() {
        stubRepositories()
        val event = catalogEvent(
            eventId = "duplicate-event",
            productId = 60L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )

        handler.handle(event)
        handler.handle(event)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("stale 이벤트(version 역전) 는 daily 에도 반영되지 않는다 (버전 가드)")
    @Test
    fun staleEventDoesNotUpsertDaily() {
        stubRepositories()
        val latest = catalogEvent(
            eventId = "latest",
            productId = 70L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 10,
            occurredAt = ZonedDateTime.parse("2026-04-10T12:00:00+09:00"),
        )
        val stale = catalogEvent(
            eventId = "stale",
            productId = 70L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 5,
            occurredAt = ZonedDateTime.parse("2026-04-10T11:00:00+09:00"),
        )

        handler.handle(latest)
        handler.handle(stale)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("occurredAt 이 UTC 여도 Asia/Seoul 기준 일자로 변환되어 upsert 된다")
    @Test
    fun normalizesOccurredAtToAsiaSeoulDate() {
        stubRepositories()
        // UTC 2026-04-10T15:00:00Z == KST 2026-04-11T00:00:00+09:00
        val event = catalogEvent(
            eventId = "utc-boundary",
            productId = 80L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.parse("2026-04-10T15:00:00Z"),
        )

        handler.handle(event)

        verify(exactly = 1) {
            productMetricsDailyJpaRepository.upsert(
                productId = 80L,
                metricDate = LocalDate.of(2026, 4, 11),
                likesDelta = 0,
                viewsDelta = 1,
                salesDelta = 0,
            )
        }
    }

    private fun stubRepositories() {
        handledEventIds.clear()
        metricsStore.clear()
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
