package com.loopers.application.metrics

import com.loopers.domain.event.FakeEventHandledRepository
import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.metrics.FakeProductMetricsRepository
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.ranking.FakeFailedScoreUpdateRepository
import com.loopers.domain.ranking.FakeRankingScoreRepository
import com.loopers.domain.ranking.RankingWeight
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class UpdateProductMetricsUseCaseTest {

    private lateinit var productMetricsRepository: FakeProductMetricsRepository
    private lateinit var eventHandledRepository: FakeEventHandledRepository
    private lateinit var rankingScoreRepository: FakeRankingScoreRepository
    private lateinit var failedScoreUpdateRepository: FakeFailedScoreUpdateRepository
    private lateinit var useCase: UpdateProductMetricsUseCase

    private val fixedDate = LocalDate.of(2026, 4, 7)
    private val fixedClock = Clock.fixed(
        fixedDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
        ZoneId.of("Asia/Seoul"),
    )

    @BeforeEach
    fun setUp() {
        productMetricsRepository = FakeProductMetricsRepository()
        eventHandledRepository = FakeEventHandledRepository()
        rankingScoreRepository = FakeRankingScoreRepository()
        failedScoreUpdateRepository = FakeFailedScoreUpdateRepository()
        useCase = UpdateProductMetricsUseCase(
            productMetricsRepository,
            eventHandledRepository,
            rankingScoreRepository,
            failedScoreUpdateRepository,
            fixedClock,
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
        fun `LIKE_REMOVED 이벤트 시 likeCount가 0이어도 랭킹 점수 -0_2가 반영된다`() {
            productMetricsRepository.save(ProductMetrics(productId = 1L, likeCount = 0))

            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.LIKE_REMOVED, 1L)

            assertThat(rankingScoreRepository.getScore(1L))
                .isCloseTo(RankingWeight.LIKE * -1, Offset.offset(0.001))
            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.likeCount).isEqualTo(0)
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

    @Nested
    @DisplayName("가중치 우선순위")
    inner class WeightPriority {

        @Test
        @DisplayName("주문 1건(0.7)이 좋아요 3건(0.6)보다 높은 랭킹 점수를 받는다")
        fun `주문 1건이 좋아요 3건보다 높은 점수를 받는다`() {
            // Arrange & Act — 상품A: 주문 1건
            useCase.handleOrderEvent("evt-order-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 1L)

            // 상품B: 좋아요 3건
            useCase.handleCatalogEvent("evt-like-1", UpdateProductMetricsUseCase.LIKE_ADDED, 2L)
            useCase.handleCatalogEvent("evt-like-2", UpdateProductMetricsUseCase.LIKE_ADDED, 2L)
            useCase.handleCatalogEvent("evt-like-3", UpdateProductMetricsUseCase.LIKE_ADDED, 2L)

            // Assert — ORDER(0.7) × 1 = 0.7 > LIKE(0.2) × 3 = 0.6
            val orderScore = rankingScoreRepository.getScore(1L)
            val likeScore = rankingScoreRepository.getScore(2L)
            assertThat(orderScore).isCloseTo(RankingWeight.ORDER, Offset.offset(0.001))
            assertThat(likeScore).isCloseTo(RankingWeight.LIKE * 3, Offset.offset(0.001))
            assertThat(orderScore).isGreaterThan(likeScore)
        }
    }

    @Nested
    @DisplayName("afterCommit best-effort")
    inner class AfterCommitBestEffort {

        @BeforeEach
        fun initSynchronization() {
            TransactionSynchronizationManager.initSynchronization()
        }

        @AfterEach
        fun clearSynchronization() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }

        private fun fireAfterCommitCallbacks() {
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        }

        @Test
        @DisplayName("Redis 실패 시 예외가 전파되지 않는다")
        fun `Redis 실패 시 예외 비전파`() {
            // Arrange
            rankingScoreRepository.failuresRemaining = 10

            // Act — 예외가 전파되지 않아야 한다
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
            fireAfterCommitCallbacks()

            // Assert — 점수 미반영, 예외도 미전파
            assertThat(rankingScoreRepository.getScore(1L)).isEqualTo(0.0)
        }

        @Test
        @DisplayName("Redis 실패 시 FailedScoreUpdate가 유지된다")
        fun `Redis 실패 시 FailedScoreUpdate가 유지된다`() {
            // Arrange
            rankingScoreRepository.failuresRemaining = 10

            // Act
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
            fireAfterCommitCallbacks()

            // Assert — 트랜잭션에서 저장되고, Redis 실패로 삭제되지 않음
            val failures = failedScoreUpdateRepository.findAll()
            assertThat(failures).hasSize(1)
            assertThat(failures[0].eventId).isEqualTo("evt-1")
            assertThat(failures[0].productId).isEqualTo(1L)
            assertThat(failures[0].score).isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
            assertThat(failures[0].rankingDate).isEqualTo(fixedDate)
        }

        @Test
        @DisplayName("Redis 성공 시 FailedScoreUpdate가 삭제된다")
        fun `Redis 성공 시 FailedScoreUpdate가 삭제된다`() {
            // Arrange — Redis 정상

            // Act
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
            fireAfterCommitCallbacks()

            // Assert — 트랜잭션에서 저장 후 afterCommit에서 삭제됨
            assertThat(failedScoreUpdateRepository.findAll()).isEmpty()
        }

        @Test
        @DisplayName("주문 이벤트 Redis 실패 시 FailedScoreUpdate가 유지된다")
        fun `주문 이벤트 Redis 실패 시 FailedScoreUpdate가 유지된다`() {
            // Arrange
            rankingScoreRepository.failuresRemaining = 10

            // Act
            useCase.handleOrderEvent("evt-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, 1L, 2L)
            fireAfterCommitCallbacks()

            // Assert — 점수 미반영, FailedScoreUpdate 유지
            assertThat(rankingScoreRepository.getScore(1L)).isEqualTo(0.0)
            val failures = failedScoreUpdateRepository.findAll()
            assertThat(failures).hasSize(1)
            assertThat(failures[0].eventId).isEqualTo("evt-1")
            assertThat(failures[0].score).isCloseTo(RankingWeight.ORDER * 2, Offset.offset(0.001))
        }
    }
}
