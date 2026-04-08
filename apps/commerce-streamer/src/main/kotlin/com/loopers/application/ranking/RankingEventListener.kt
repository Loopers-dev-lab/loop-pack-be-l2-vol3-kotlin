package com.loopers.application.ranking

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RankingEventListener(
    private val rankingScoreAccumulator: RankingScoreAccumulator,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun handle(event: RankingScoreEvent) {
        try {
            when (event) {
                is RankingScoreEvent.LikeAdded -> rankingScoreAccumulator.addLikeScore(event.productId)
                is RankingScoreEvent.LikeCancelled -> rankingScoreAccumulator.cancelLikeScore(event.productId)
                is RankingScoreEvent.OrderCreated -> rankingScoreAccumulator.addOrderScore(event.productIds, event.totalAmount)
                is RankingScoreEvent.PaymentApproved -> rankingScoreAccumulator.addPaymentScore(event.productIds, event.amount)
            }
        } catch (e: Exception) {
            log.error("랭킹 점수 적재 실패 [event={}]", event, e)
        }
    }
}
