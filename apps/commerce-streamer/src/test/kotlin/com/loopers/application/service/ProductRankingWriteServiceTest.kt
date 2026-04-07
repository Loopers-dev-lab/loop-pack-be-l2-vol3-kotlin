package com.loopers.application.service

import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.OrderLineItem
import com.loopers.domain.event.ProductViewedEvent
import com.loopers.domain.ranking.DefaultScoringStrategy
import com.loopers.domain.ranking.ProductRankingWriteRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ProductRankingWriteService 단위 테스트")
class ProductRankingWriteServiceTest {

    private lateinit var productRankingWriteRepository: ProductRankingWriteRepository
    private lateinit var service: ProductRankingWriteService

    @BeforeEach
    fun setUp() {
        productRankingWriteRepository = mockk(relaxed = true)
        service = ProductRankingWriteService(productRankingWriteRepository, DefaultScoringStrategy())
    }

    @Test
    @DisplayName("조회 이벤트는 viewScore(0.1)를 반영한다")
    fun writesViewScore() {
        service.write(ProductViewedEvent(productId = 101L, userId = 1L, dedupeKey = "view-1"))

        verify {
            productRankingWriteRepository.incrementScore(any<LocalDate>(), 101L, 0.1)
        }
    }

    @Test
    @DisplayName("좋아요 증가는 likeScore(0.2), 감소는 -likeScore(-0.2)")
    fun writesLikeScores() {
        service.write(LikeCountEvent(productId = 101L, type = LikeCountEventType.INCREMENT, dedupeKey = "like-up"))
        service.write(LikeCountEvent(productId = 101L, type = LikeCountEventType.DECREMENT, dedupeKey = "like-down"))

        verify {
            productRankingWriteRepository.incrementScore(any<LocalDate>(), 101L, 0.2)
            productRankingWriteRepository.incrementScore(any<LocalDate>(), 101L, -0.2)
        }
    }

    @Test
    @DisplayName("주문 1건 점수(0.7)는 좋아요 3건(0.6)보다 크다")
    fun orderOneBeatsThreeLikes() {
        service.write(
            OrderCreatedEvent(
                orderId = 1L,
                lineItems = listOf(OrderLineItem(productId = 101L, quantity = 1)),
                dedupeKey = "order-1",
            ),
        )

        verify {
            productRankingWriteRepository.incrementScore(any<LocalDate>(), 101L, 0.7)
        }
    }
}
