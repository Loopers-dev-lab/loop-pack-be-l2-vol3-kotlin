package com.loopers.application.ranking

sealed class RankingScoreEvent {
    data class LikeAdded(val productId: Long) : RankingScoreEvent()
    data class LikeCancelled(val productId: Long) : RankingScoreEvent()
    data class OrderCreated(val productIds: List<Long>, val totalAmount: Long) : RankingScoreEvent()
    data class PaymentApproved(val productIds: List<Long>, val amount: Long) : RankingScoreEvent()
}
