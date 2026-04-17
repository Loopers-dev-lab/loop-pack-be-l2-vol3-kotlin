package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLogRepository
import com.loopers.domain.ranking.RankingRedisOperations
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.ViewDedupOperations
import com.loopers.domain.ranking.ViewRateOperations
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
    private lateinit var rankingRedisOperations: RankingRedisOperations

    @Mock
    private lateinit var rankingScorePolicy: RankingScorePolicy

    @Mock
    private lateinit var rankingWeightProvider: RankingWeightProvider

    @Mock
    private lateinit var rankingEventLogRepository: RankingEventLogRepository

    @Mock
    private lateinit var viewDedupOperations: ViewDedupOperations

    @Mock
    private lateinit var viewRateOperations: ViewRateOperations

    @InjectMocks
    private lateinit var rankingAggregationService: RankingAggregationService

    private val testDate = LocalDate.of(2026, 4, 8)
    private val testDateTime = LocalDateTime.of(2026, 4, 8, 14, 30)
    private val testEventId = "test-event-123"
    private val normalContext = ViewEventContext(loginId = "user1", clientIp = "1.2.3.4", userAgent = "Mozilla", referer = "https://example.com")

    @DisplayName("조회 이벤트를 처리할 때,")
    @Nested
    inner class ProcessViewEvent {

        @DisplayName("중복이 아니면 Trust Score 적용 후 점수를 반영한다.")
        @Test
        fun appliesTrustScore_whenNotDuplicate() {
            // arrange
            whenever(viewDedupOperations.isDuplicate(any(), any(), any(), any())).thenReturn(false)
            whenever(rankingWeightProvider.getViewWeight()).thenReturn(0.1)
            whenever(rankingScorePolicy.calculateViewScore(0.1)).thenReturn(0.1)
            whenever(viewRateOperations.incrementAndGetRequestCount(any(), any())).thenReturn(1L)
            whenever(viewRateOperations.addViewedProductAndGetCount(any(), any(), any())).thenReturn(3L)

            // act
            rankingAggregationService.processViewEvent(101L, testDate, testDateTime, testEventId, normalContext)

            // assert
            verify(rankingRedisOperations).incrementScore(eq(101L), any(), eq(testDate))
            verify(viewDedupOperations).markViewed(101L, "user1", "1.2.3.4", testDate)
            verify(rankingEventLogRepository).save(any())
        }

        @DisplayName("중복이면 점수를 반영하지 않고 false를 반환한다.")
        @Test
        fun skips_whenDuplicate() {
            // arrange
            whenever(viewDedupOperations.isDuplicate(any(), any(), any(), any())).thenReturn(true)

            // act
            val result = rankingAggregationService.processViewEvent(101L, testDate, testDateTime, testEventId, normalContext)

            // assert
            assert(!result)
            verify(rankingRedisOperations, org.mockito.kotlin.never()).incrementScore(any(), any(), any())
        }
    }

    @DisplayName("주문 이벤트를 처리할 때,")
    @Nested
    inner class ProcessOrderEvent {

        @DisplayName("상품별로 점수를 반영한다.")
        @Test
        fun processesPerProduct() {
            // arrange
            val items = listOf(OrderItemScore(productId = 1L, amount = BigDecimal("10000")))
            whenever(rankingWeightProvider.getOrderWeight()).thenReturn(0.6)
            whenever(rankingScorePolicy.calculateOrderScore(BigDecimal("10000"), 0.6)).thenReturn(6000.0)

            // act
            rankingAggregationService.processOrderEvent(items, testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisOperations).incrementScore(1L, 6000.0, testDate)
            verify(rankingEventLogRepository).save(any())
        }

        @DisplayName("빈 항목이면 아무 처리도 하지 않는다.")
        @Test
        fun doesNothing_whenEmpty() {
            // act
            rankingAggregationService.processOrderEvent(emptyList(), testDate, testDateTime, testEventId)

            // assert
            verify(rankingRedisOperations, org.mockito.kotlin.never()).incrementScore(any(), any(), any())
        }
    }
}
