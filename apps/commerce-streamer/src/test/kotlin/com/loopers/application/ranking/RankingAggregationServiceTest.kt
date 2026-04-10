package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLogRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.ViewTrustScoreCalculator
import com.loopers.infrastructure.ranking.RankingRedisRepository
import com.loopers.infrastructure.ranking.ViewRateRedisRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RankingAggregationServiceTest {

    @Mock
    private lateinit var rankingRedisRepository: RankingRedisRepository

    @Mock
    private lateinit var rankingScorePolicy: RankingScorePolicy

    @Mock
    private lateinit var rankingWeightProvider: RankingWeightProvider

    @Mock
    private lateinit var rankingEventLogRepository: RankingEventLogRepository

    @Mock
    private lateinit var viewTrustScoreCalculator: ViewTrustScoreCalculator

    @Mock
    private lateinit var viewRateRedisRepository: ViewRateRedisRepository

    @InjectMocks
    private lateinit var rankingAggregationService: RankingAggregationService

    private val testDate = LocalDate.of(2026, 4, 8)
    private val testDateTime = LocalDateTime.of(2026, 4, 8, 14, 30)
    private val testEventId = "test-event-123"

    @DisplayName("조회 이벤트를 처리할 때,")
    @Nested
    inner class ProcessViewEvent {

        @DisplayName("Trust Score가 적용된 점수로 ZSET에 적재한다.")
        @Test
        fun appliesTrustScore_whenViewEvent() {
            // arrange
            val payload = mapOf<String, Any?>("loginId" to "user1", "clientIp" to "1.2.3.4", "userAgent" to "Mozilla", "referer" to "https://example.com")
            whenever(rankingWeightProvider.getViewWeight()).thenReturn(0.1)
            whenever(rankingScorePolicy.calculateViewScore(0.1)).thenReturn(0.1)
            whenever(viewRateRedisRepository.incrementAndGetRequestCount(any(), any())).thenReturn(1L)
            whenever(viewRateRedisRepository.addViewedProductAndGetCount(any(), any(), any())).thenReturn(3L)
            whenever(viewTrustScoreCalculator.calculate(any())).thenReturn(1.0)

            // act
            rankingAggregationService.processViewEvent(101L, testDate, testDateTime, testEventId, payload)

            // assert — 0.1 * 1.0 = 0.1
            verify(rankingRedisRepository).incrementScore(101L, 0.1, testDate)
            verify(rankingEventLogRepository).save(any())
        }

        @DisplayName("Trust Score가 낮으면 낮은 점수가 적용된다.")
        @Test
        fun appliesLowScore_whenLowTrustScore() {
            // arrange
            val payload = mapOf<String, Any?>("loginId" to null, "clientIp" to "1.2.3.4", "userAgent" to null, "referer" to null)
            whenever(rankingWeightProvider.getViewWeight()).thenReturn(0.1)
            whenever(rankingScorePolicy.calculateViewScore(0.1)).thenReturn(0.1)
            whenever(viewRateRedisRepository.incrementAndGetRequestCount(any(), any())).thenReturn(15L)
            whenever(viewRateRedisRepository.addViewedProductAndGetCount(any(), any(), any())).thenReturn(1L)
            whenever(viewTrustScoreCalculator.calculate(any())).thenReturn(0.05)

            // act
            rankingAggregationService.processViewEvent(101L, testDate, testDateTime, testEventId, payload)

            // assert — 0.1 * 0.05 = 0.005
            verify(rankingRedisRepository).incrementScore(eq(101L), eq(0.005000000000000001), eq(testDate))
        }
    }

    @DisplayName("좋아요 이벤트를 처리할 때,")
    @Nested
    inner class ProcessLikeEvent {

        @DisplayName("가중치가 적용된 점수로 ZSET에 적재한다.")
        @Test
        fun processesLikeEventFully() {
            // arrange
            whenever(rankingWeightProvider.getLikeWeight()).thenReturn(0.2)
            whenever(rankingScorePolicy.calculateLikeScore(0.2)).thenReturn(0.2)

            // act
            rankingAggregationService.processLikeEvent(101L, testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository).incrementScore(101L, 0.2, testDate)
            verify(rankingEventLogRepository).save(any())
        }
    }

    @DisplayName("주문 이벤트를 처리할 때,")
    @Nested
    inner class ProcessOrderEvent {

        @DisplayName("상품별로 Redis 적재 + 이벤트 로그 저장을 수행한다.")
        @Test
        fun processesOrderEventPerProduct() {
            // arrange
            val items = listOf(
                OrderItemScore(productId = 1L, amount = BigDecimal("10000")),
            )
            whenever(rankingWeightProvider.getOrderWeight()).thenReturn(0.6)
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("10000"), 0.6)).thenReturn(6000.0)

            // act
            rankingAggregationService.processOrderEvent(items, testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository).incrementScore(1L, 6000.0, testDate)
            verify(rankingEventLogRepository).save(any())
        }

        @DisplayName("주문 항목이 비어있으면, 아무 처리도 하지 않는다.")
        @Test
        fun doesNothing_whenEmptyItems() {
            // act
            rankingAggregationService.processOrderEvent(emptyList(), testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository, org.mockito.kotlin.never()).incrementScore(any(), any(), any())
        }
    }
}
