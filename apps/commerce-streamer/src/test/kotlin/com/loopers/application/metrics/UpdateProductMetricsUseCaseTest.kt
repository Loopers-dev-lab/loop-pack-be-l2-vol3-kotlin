package com.loopers.application.metrics

import com.loopers.domain.event.FakeEventHandledRepository
import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.metrics.FakeProductMetricsRepository
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.ranking.FakeRankingScoreRepository
import com.loopers.domain.ranking.RankingWeight
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateProductMetricsUseCaseTest {

    private lateinit var productMetricsRepository: FakeProductMetricsRepository
    private lateinit var eventHandledRepository: FakeEventHandledRepository
    private lateinit var rankingScoreRepository: FakeRankingScoreRepository
    private lateinit var useCase: UpdateProductMetricsUseCase

    @BeforeEach
    fun setUp() {
        productMetricsRepository = FakeProductMetricsRepository()
        eventHandledRepository = FakeEventHandledRepository()
        rankingScoreRepository = FakeRankingScoreRepository()
        useCase = UpdateProductMetricsUseCase(
            productMetricsRepository,
            eventHandledRepository,
            rankingScoreRepository,
        )
    }

    @Nested
    @DisplayName("handleCatalogEvent 시")
    inner class HandleCatalogEvent {

        @Test
        fun `ProductViewed 이벤트를 처리하면 viewCount가 증가한다`() {
            useCase.handleCatalogEvent(
                eventId = "evt-1",
                eventType = UpdateProductMetricsUseCase.PRODUCT_VIEWED,
                productId = 1L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.viewCount).isEqualTo(1)
        }

        @Test
        fun `LikeAdded 이벤트를 처리하면 likeCount가 증가한다`() {
            useCase.handleCatalogEvent(
                eventId = "evt-1",
                eventType = UpdateProductMetricsUseCase.LIKE_ADDED,
                productId = 1L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.likeCount).isEqualTo(1)
        }

        @Test
        fun `LikeRemoved 이벤트를 처리하면 likeCount가 감소한다`() {
            productMetricsRepository.save(ProductMetrics(productId = 1L, likeCount = 5))

            useCase.handleCatalogEvent(
                eventId = "evt-1",
                eventType = UpdateProductMetricsUseCase.LIKE_REMOVED,
                productId = 1L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.likeCount).isEqualTo(4)
        }

        @Test
        fun `같은 상품에 여러 이벤트를 누적 처리한다`() {
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
            useCase.handleCatalogEvent("evt-2", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
            useCase.handleCatalogEvent("evt-3", UpdateProductMetricsUseCase.LIKE_ADDED, 1L)

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.viewCount).isEqualTo(2)
            assertThat(metrics?.likeCount).isEqualTo(1)
        }

        @Test
        fun `이미 처리된 eventId는 무시한다`() {
            eventHandledRepository.save(EventHandled(eventId = "evt-1"))

            useCase.handleCatalogEvent(
                eventId = "evt-1",
                eventType = UpdateProductMetricsUseCase.PRODUCT_VIEWED,
                productId = 1L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics).isNull()
        }

        @Test
        fun `알 수 없는 eventType은 무시한다`() {
            useCase.handleCatalogEvent(
                eventId = "evt-1",
                eventType = "UnknownType",
                productId = 1L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics).isNull()
            assertThat(eventHandledRepository.existsByEventId("evt-1")).isTrue()
        }

        @Test
        fun `처리 후 EventHandled가 저장된다`() {
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)

            assertThat(eventHandledRepository.existsByEventId("evt-1")).isTrue()
        }

        @Test
        fun `PRODUCT_VIEWED 이벤트 시 랭킹 점수 +0_1이 반영된다`() {
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)

            assertThat(rankingScoreRepository.getScore(1L))
                .isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
        }

        @Test
        fun `LIKE_ADDED 이벤트 시 랭킹 점수 +0_2가 반영된다`() {
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.LIKE_ADDED, 1L)

            assertThat(rankingScoreRepository.getScore(1L))
                .isCloseTo(RankingWeight.LIKE, Offset.offset(0.001))
        }

        @Test
        fun `LIKE_REMOVED 이벤트 시 랭킹 점수 -0_2가 반영된다`() {
            productMetricsRepository.save(ProductMetrics(productId = 1L, likeCount = 5))

            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.LIKE_REMOVED, 1L)

            assertThat(rankingScoreRepository.getScore(1L))
                .isCloseTo(RankingWeight.LIKE * -1, Offset.offset(0.001))
        }

        @Test
        fun `이미 처리된 이벤트는 랭킹 점수도 갱신하지 않는다`() {
            eventHandledRepository.save(EventHandled(eventId = "evt-1"))

            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)

            assertThat(rankingScoreRepository.getScore(1L)).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("handleOrderEvent 시")
    inner class HandleOrderEvent {

        @Test
        fun `PaymentCompleted 이벤트를 처리하면 salesCount가 증가한다`() {
            useCase.handleOrderEvent(
                eventId = "evt-1",
                eventType = UpdateProductMetricsUseCase.PAYMENT_COMPLETED,
                productId = 1L,
                quantity = 2L,
            )

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.salesCount).isEqualTo(2)
        }

        @Test
        fun `이미 처리된 eventId는 무시한다`() {
            eventHandledRepository.save(EventHandled(eventId = "evt-1"))

            useCase.handleOrderEvent("evt-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 1L)

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics).isNull()
        }

        @Test
        fun `PaymentCompleted가 아닌 eventType은 무시한다`() {
            useCase.handleOrderEvent("evt-1", "PaymentFailed", 1L, 1L)

            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics).isNull()
            assertThat(eventHandledRepository.existsByEventId("evt-1")).isTrue()
        }

        @Test
        fun `처리 후 EventHandled가 저장된다`() {
            useCase.handleOrderEvent("evt-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 1L)

            assertThat(eventHandledRepository.existsByEventId("evt-1")).isTrue()
        }

        @Test
        fun `PAYMENT_COMPLETED 이벤트 시 랭킹 점수 +0_7 x quantity가 반영된다`() {
            useCase.handleOrderEvent("evt-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 3L)

            assertThat(rankingScoreRepository.getScore(1L))
                .isCloseTo(RankingWeight.ORDER * 3, Offset.offset(0.001))
        }

        @Test
        fun `이미 처리된 이벤트는 랭킹 점수도 갱신하지 않는다`() {
            eventHandledRepository.save(EventHandled(eventId = "evt-1"))

            useCase.handleOrderEvent("evt-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 2L)

            assertThat(rankingScoreRepository.getScore(1L)).isEqualTo(0.0)
        }
    }
}
