package com.loopers.application.event

import com.loopers.application.metrics.ProductMetricsUpdater
import com.loopers.application.ranking.RankingUpdater
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ProductAggregationListenerTest {
    private val productMetricsUpdater = mockk<ProductMetricsUpdater>(relaxed = true)
    private val rankingUpdater = mockk<RankingUpdater>(relaxed = true)
    private val listener = ProductAggregationListener(productMetricsUpdater, rankingUpdater)

    @Test
    fun `상품_조회_이벤트는_집계와_랭킹을_함께_갱신한다`() {
        val occurredAt = ZonedDateTime.now()

        listener.handle(ProductViewedEvent(productId = 1L, occurredAt = occurredAt))

        verify(exactly = 1) { productMetricsUpdater.increaseViewCount(1L, occurredAt) }
        verify(exactly = 1) { rankingUpdater.applyViewed(1L, occurredAt) }
    }

    @Test
    fun `좋아요_변경_이벤트는_집계와_랭킹을_함께_갱신한다`() {
        val occurredAt = ZonedDateTime.now()

        listener.handle(ProductLikeChangedEvent(productId = 2L, brandId = 10L, delta = -1L, occurredAt = occurredAt))

        verify(exactly = 1) { productMetricsUpdater.decreaseLikeCount(2L, occurredAt) }
        verify(exactly = 1) { rankingUpdater.applyLikeChanged(2L, -1L, occurredAt) }
    }

    @Test
    fun `주문_완료_이벤트는_상품별로_집계와_랭킹을_반영한다`() {
        val occurredAt = ZonedDateTime.now()

        listener.handle(
            OrderPaidEvent(
                orderId = 100L,
                memberId = 7L,
                items = listOf(
                    OrderPaidEvent.Item(productId = 11L, quantity = 2L),
                    OrderPaidEvent.Item(productId = 12L, quantity = 1L),
                ),
                occurredAt = occurredAt,
            ),
        )

        verify(exactly = 1) { productMetricsUpdater.increaseSalesCount(11L, 2L, occurredAt) }
        verify(exactly = 1) { productMetricsUpdater.increaseSalesCount(12L, 1L, occurredAt) }
        verify(exactly = 1) { rankingUpdater.applyOrdered(11L, 2L, occurredAt) }
        verify(exactly = 1) { rankingUpdater.applyOrdered(12L, 1L, occurredAt) }
    }
}
