package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLogRepository
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

    @Mock
    private lateinit var rankingWeightProvider: RankingWeightProvider

    @Mock
    private lateinit var rankingEventLogRepository: RankingEventLogRepository

    @InjectMocks
    private lateinit var rankingAggregationService: RankingAggregationService

    private val testDate = LocalDate.of(2026, 4, 8)
    private val testDateTime = LocalDateTime.of(2026, 4, 8, 14, 30)
    private val testEventId = "test-event-123"

    @DisplayName("조회 이벤트를 처리할 때,")
    @Nested
    inner class ProcessViewEvent {

        @DisplayName("가중치 적용 + Redis 적재 + 이벤트 로그 저장을 수행한다.")
        @Test
        fun processesViewEventFully() {
            // arrange
            whenever(rankingWeightProvider.getViewWeight()).thenReturn(0.15)
            whenever(rankingScorePolicy.calculateViewScore(0.15)).thenReturn(0.15)

            // act
            rankingAggregationService.processViewEvent(101L, testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository).incrementScore(101L, 0.15, testDate)
            verify(rankingRedisRepository).incrementHourlyScore(101L, 0.15, testDateTime)
            verify(rankingEventLogRepository).save(any())
        }
    }

    @DisplayName("좋아요 이벤트를 처리할 때,")
    @Nested
    inner class ProcessLikeEvent {

        @DisplayName("가중치 적용 + Redis 적재 + 이벤트 로그 저장을 수행한다.")
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
                OrderItemScore(productId = 2L, amount = BigDecimal("5000")),
            )
            whenever(rankingWeightProvider.getOrderWeight()).thenReturn(0.6)
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("10000"), 0.6)).thenReturn(6000.0)
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("5000"), 0.6)).thenReturn(3000.0)

            // act
            rankingAggregationService.processOrderEvent(items, testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository).incrementScore(1L, 6000.0, testDate)
            verify(rankingRedisRepository).incrementScore(2L, 3000.0, testDate)
            verify(rankingEventLogRepository, org.mockito.kotlin.times(2)).save(any())
        }

        @DisplayName("주문 항목이 비어있으면, 아무 처리도 하지 않는다.")
        @Test
        fun doesNothing_whenEmptyItems() {
            // act
            rankingAggregationService.processOrderEvent(emptyList(), testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisRepository, org.mockito.kotlin.never()).incrementScore(any(), any(), any())
            verify(rankingEventLogRepository, org.mockito.kotlin.never()).save(any())
        }
    }
}
