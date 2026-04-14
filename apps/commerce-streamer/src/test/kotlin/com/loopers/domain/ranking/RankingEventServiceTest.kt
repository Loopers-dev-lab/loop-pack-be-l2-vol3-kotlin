package com.loopers.domain.ranking

import com.loopers.domain.metrics.EventHandledRecord
import com.loopers.domain.metrics.EventHandledRepository
import com.loopers.domain.metrics.OrderItemMetrics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RankingEventServiceTest {
    private lateinit var rankingEventRepository: RankingEventRepository
    private lateinit var eventHandledRepository: EventHandledRepository
    private lateinit var rankingEventService: RankingEventService

    @BeforeEach
    fun setUp() {
        rankingEventRepository = mock()
        eventHandledRepository = mock()
        rankingEventService = RankingEventService(rankingEventRepository, eventHandledRepository)

        whenever(rankingEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(rankingEventRepository.saveAll(any())).thenAnswer { it.arguments[0] }
        whenever(eventHandledRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @DisplayName("조회 배치 이벤트를 저장할 때, ")
    @Nested
    inner class SaveViewBatch {
        @DisplayName("상품별로 가중치가 적용된 점수로 저장한다.")
        @Test
        fun savesWithWeightedScore() {
            // arrange
            val views = listOf(
                ViewCount(productId = 1L, count = 100),
                ViewCount(productId = 2L, count = 50),
            )

            // act
            rankingEventService.saveViewBatch(views, "batch-uuid-1")

            // assert
            val captor = argumentCaptor<List<RankingEvent>>()
            verify(rankingEventRepository).saveAll(captor.capture())
            val saved = captor.firstValue
            assertThat(saved).hasSize(2)
            assertThat(saved[0].productId).isEqualTo(1L)
            assertThat(saved[0].score).isEqualTo(0.1 * 100)
            assertThat(saved[0].rawCount).isEqualTo(100)
            assertThat(saved[0].eventType).isEqualTo(RankingEventType.VIEW)
            assertThat(saved[1].productId).isEqualTo(2L)
            assertThat(saved[1].score).isEqualTo(0.1 * 50)
        }
    }

    @DisplayName("좋아요 이벤트를 저장할 때, ")
    @Nested
    inner class SaveLikeEvent {
        @DisplayName("가중치 0.2가 적용된 점수로 저장한다.")
        @Test
        fun savesWithLikeWeight() {
            // arrange
            whenever(eventHandledRepository.existsByEventId(1L)).thenReturn(false)

            // act
            rankingEventService.saveLikeEvent(productId = 10L, eventId = 1L)

            // assert
            val captor = argumentCaptor<RankingEvent>()
            verify(rankingEventRepository).save(captor.capture())
            assertThat(captor.firstValue.productId).isEqualTo(10L)
            assertThat(captor.firstValue.score).isEqualTo(0.2)
            assertThat(captor.firstValue.eventType).isEqualTo(RankingEventType.LIKE)
        }

        @DisplayName("이미 처리된 이벤트는 무시한다.")
        @Test
        fun skipsAlreadyHandled() {
            // arrange
            whenever(eventHandledRepository.existsByEventId(1L)).thenReturn(true)

            // act
            rankingEventService.saveLikeEvent(productId = 10L, eventId = 1L)

            // assert
            verify(rankingEventRepository, never()).save(any())
        }
    }

    @DisplayName("주문 이벤트를 저장할 때, ")
    @Nested
    inner class SaveOrderEvent {
        @DisplayName("아이템별로 가중치 0.6 × price × quantity 점수로 저장한다.")
        @Test
        fun savesWithOrderWeight() {
            // arrange
            whenever(eventHandledRepository.existsByEventId(1L)).thenReturn(false)
            val items = listOf(
                OrderItemMetrics(productId = 1L, productPrice = 10000, quantity = 2),
                OrderItemMetrics(productId = 2L, productPrice = 5000, quantity = 1),
            )

            // act
            rankingEventService.saveOrderEvent(items, eventId = 1L)

            // assert
            val captor = argumentCaptor<List<RankingEvent>>()
            verify(rankingEventRepository).saveAll(captor.capture())
            val saved = captor.firstValue
            assertThat(saved).hasSize(2)
            assertThat(saved[0].productId).isEqualTo(1L)
            assertThat(saved[0].score).isEqualTo(0.6 * 10000 * 2)
            assertThat(saved[0].eventType).isEqualTo(RankingEventType.ORDER)
            assertThat(saved[1].productId).isEqualTo(2L)
            assertThat(saved[1].score).isEqualTo(0.6 * 5000 * 1)
        }

        @DisplayName("이미 처리된 이벤트는 무시한다.")
        @Test
        fun skipsAlreadyHandled() {
            // arrange
            whenever(eventHandledRepository.existsByEventId(1L)).thenReturn(true)

            // act
            rankingEventService.saveOrderEvent(emptyList(), eventId = 1L)

            // assert
            verify(rankingEventRepository, never()).saveAll(any())
        }
    }
}
