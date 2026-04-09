package com.loopers.application.ranking

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.math.log10

@Component
class RankingEventListener(
    private val rankingScoreBuffer: RankingScoreBuffer,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun handle(event: RankingScoreEvent) {
        try {
            when (event) {
                is RankingScoreEvent.ProductViewed ->
                    rankingScoreBuffer.add(event.productId, rankingProperties.weight.view)
                is RankingScoreEvent.LikeAdded ->
                    rankingScoreBuffer.add(event.productId, rankingProperties.weight.like)
                is RankingScoreEvent.LikeCancelled ->
                    rankingScoreBuffer.add(event.productId, -rankingProperties.weight.like)
                is RankingScoreEvent.OrderCreated ->
                    addOrderScores(event.productIds, event.totalAmount)
                is RankingScoreEvent.PaymentApproved ->
                    addOrderScores(event.productIds, event.amount)
            }
        } catch (e: Exception) {
            log.error("랭킹 점수 버퍼 누적 실패 [event={}]", event, e)
        }
    }

    private fun addOrderScores(productIds: List<Long>, amount: Long) {
        if (productIds.isEmpty()) return
        val revenuePerProduct = amount / productIds.size
        val increment = rankingProperties.weight.order * log10(1.0 + revenuePerProduct)
        productIds.forEach { productId ->
            rankingScoreBuffer.add(productId, increment)
        }
    }
}
