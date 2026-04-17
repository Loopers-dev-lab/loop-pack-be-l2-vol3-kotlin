package com.loopers.application.ranking

import com.loopers.application.metrics.ProductCountBuffer
import com.loopers.hash.MetricType
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.math.log10

@Component
class RankingEventListener(
    private val rankingScoreBuffer: RankingScoreBuffer,
    private val productCountBuffer: ProductCountBuffer,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun handle(event: RankingScoreEvent) {
        try {
            when (event) {
                is RankingScoreEvent.ProductViewed -> {
                    rankingScoreBuffer.add(event.productId, rankingProperties.weight.view)
                    productCountBuffer.add(event.productId, MetricType.VIEW)
                }
                is RankingScoreEvent.LikeAdded -> {
                    rankingScoreBuffer.add(event.productId, rankingProperties.weight.like)
                    productCountBuffer.add(event.productId, MetricType.LIKE)
                }
                is RankingScoreEvent.LikeCancelled -> {
                    rankingScoreBuffer.add(event.productId, -rankingProperties.weight.like)
                    productCountBuffer.add(event.productId, MetricType.LIKE, -1L)
                }
                is RankingScoreEvent.OrderCreated ->
                    addOrderData(event.productIds, event.totalAmount)
                is RankingScoreEvent.PaymentApproved ->
                    addOrderData(event.productIds, event.amount)
            }
        } catch (e: Exception) {
            log.error("랭킹/카운트 버퍼 누적 실패 [event={}]", event, e)
        }
    }

    private fun addOrderData(productIds: List<Long>, amount: Long) {
        if (productIds.isEmpty()) return
        val revenuePerProduct = amount / productIds.size
        val scoreIncrement = rankingProperties.weight.order * log10(1.0 + revenuePerProduct)
        productIds.forEach { productId ->
            rankingScoreBuffer.add(productId, scoreIncrement)
            productCountBuffer.add(productId, MetricType.ORDER)
        }
    }
}
