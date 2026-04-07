package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
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

    @InjectMocks
    private lateinit var rankingAggregationService: RankingAggregationService

    private val testDate = LocalDate.of(2026, 4, 8)
    private val testDateTime = LocalDateTime.of(2026, 4, 8, 14, 30)

    @DisplayName("조회 이벤트를 처리할 때,")
    @Nested
    inner class ProcessViewEvent {

        @DisplayName("일간 + 시간 단위 ZSET에 동시 적재한다.")
        @Test
        fun incrementsBothDailyAndHourlyScore() {
            // arrange
            whenever(rankingScorePolicy.calculateViewScore()).thenReturn(0.1)

            // act
            rankingAggregationService.processViewEvent(101L, testDate, testDateTime)

            // assert
            verify(rankingRedisRepository).incrementScore(101L, 0.1, testDate)
            verify(rankingRedisRepository).incrementHourlyScore(101L, 0.1, testDateTime)
        }
    }

    @DisplayName("좋아요 이벤트를 처리할 때,")
    @Nested
    inner class ProcessLikeEvent {

        @DisplayName("일간 + 시간 단위 ZSET에 동시 적재한다.")
        @Test
        fun incrementsBothDailyAndHourlyScore() {
            // arrange
            whenever(rankingScorePolicy.calculateLikeScore()).thenReturn(0.2)

            // act
            rankingAggregationService.processLikeEvent(101L, testDate, testDateTime)

            // assert
            verify(rankingRedisRepository).incrementScore(101L, 0.2, testDate)
            verify(rankingRedisRepository).incrementHourlyScore(101L, 0.2, testDateTime)
        }
    }

    @DisplayName("주문 이벤트를 처리할 때,")
    @Nested
    inner class ProcessOrderEvent {

        @DisplayName("상품별 금액 × 가중치로 일간 + 시간 단위 ZSET에 동시 적재한다.")
        @Test
        fun incrementsBothDailyAndHourlyScore_perProduct() {
            // arrange
            val items = listOf(
                OrderItemScore(productId = 1L, amount = BigDecimal("10000")),
                OrderItemScore(productId = 2L, amount = BigDecimal("5000")),
            )
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("10000"))).thenReturn(6000.0)
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("5000"))).thenReturn(3000.0)

            // act
            rankingAggregationService.processOrderEvent(items, testDate, testDateTime)

            // assert
            verify(rankingRedisRepository).incrementScore(1L, 6000.0, testDate)
            verify(rankingRedisRepository).incrementHourlyScore(1L, 6000.0, testDateTime)
            verify(rankingRedisRepository).incrementScore(2L, 3000.0, testDate)
            verify(rankingRedisRepository).incrementHourlyScore(2L, 3000.0, testDateTime)
        }

        @DisplayName("주문 항목이 비어있으면, 아무 처리도 하지 않는다.")
        @Test
        fun doesNothing_whenEmptyItems() {
            // act
            rankingAggregationService.processOrderEvent(emptyList(), testDate, testDateTime)

            // assert
            verify(rankingRedisRepository, org.mockito.kotlin.never()).incrementScore(any(), any(), any())
            verify(rankingRedisRepository, org.mockito.kotlin.never()).incrementHourlyScore(any(), any(), any())
        }
    }
}
