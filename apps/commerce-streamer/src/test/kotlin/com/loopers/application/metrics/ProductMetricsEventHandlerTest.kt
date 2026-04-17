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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.ZonedDateTime

@DisplayName("ProductMetricsEventHandler")
class ProductMetricsEventHandlerTest {
    private val eventHandledJpaRepository: EventHandledJpaRepository = mockk()
    private val productMetricsJpaRepository: ProductMetricsJpaRepository = mockk()
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository = mockk(relaxed = true)
    private val rankingRepository: RankingRepository = mockk(relaxed = true)
    private val rankingScorePolicy = RankingScorePolicy(viewWeight = 0.1, likeWeight = 0.2, orderWeight = 0.7)
    private val productMetricsEventHandler = ProductMetricsEventHandler(
        eventHandledJpaRepository = eventHandledJpaRepository,
        productMetricsJpaRepository = productMetricsJpaRepository,
        productMetricsDailyJpaRepository = productMetricsDailyJpaRepository,
        rankingRepository = rankingRepository,
        rankingScorePolicy = rankingScorePolicy,
    )

    @DisplayName("이벤트를 처리하면 product_metrics를 upsert 한다")
    @Test
    fun upsertsProductMetrics() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val likeEvent = catalogEventMessage(
            eventId = "event-1",
            productId = 10L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 1,
        )
        val viewEvent = catalogEventMessage(
            eventId = "event-2",
            productId = 10L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 2,
        )

        productMetricsEventHandler.handle(likeEvent)
        productMetricsEventHandler.handle(viewEvent)

        val metrics = metricsStore[10L]
        assertThat(metrics).isNotNull
        assertThat(metrics!!.likesCount).isEqualTo(1)
        assertThat(metrics.viewsCount).isEqualTo(1)
    }

    @DisplayName("동일 event_id는 event_handled를 통해 중복 반영되지 않는다")
    @Test
    fun ignoresDuplicateEventByEventHandled() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val duplicateEvent = catalogEventMessage(
            eventId = "duplicate-event",
            productId = 20L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 3,
        )

        productMetricsEventHandler.handle(duplicateEvent)
        productMetricsEventHandler.handle(duplicateEvent)

        val metrics = metricsStore[20L]
        assertThat(metrics).isNotNull
        assertThat(metrics!!.likesCount).isEqualTo(1)
        assertThat(handledEventIds).containsExactly("duplicate-event")
    }

    @DisplayName("더 오래된 버전 이벤트는 최신 상태를 덮어쓰지 않는다")
    @Test
    fun ignoresStaleEventByVersionGuard() {
        val handledEventIds = mutableSetOf<String>()
        val metricsStore = mutableMapOf<Long, ProductMetricsModel>()
        stubRepositories(handledEventIds, metricsStore)

        val latestEvent = catalogEventMessage(
            eventId = "latest-event",
            productId = 30L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 10,
        )
        val staleEvent = catalogEventMessage(
            eventId = "stale-event",
            productId = 30L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 9,
            version = 9,
        )

        productMetricsEventHandler.handle(latestEvent)
        productMetricsEventHandler.handle(staleEvent)

        val metrics = metricsStore[30L]
        assertThat(metrics).isNotNull
        assertThat(metrics!!.likesCount).isEqualTo(1)
        assertThat(metrics.lastEventVersion).isEqualTo(10)
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

    private fun catalogEventMessage(
        eventId: String,
        productId: Long,
        eventType: CatalogEventType,
        delta: Long,
        version: Long,
    ): CatalogEventMessage {
        return CatalogEventMessage(
            eventId = eventId,
            productId = productId,
            eventType = eventType,
            delta = delta,
            version = version,
            occurredAt = ZonedDateTime.now(),
        )
    }
}
