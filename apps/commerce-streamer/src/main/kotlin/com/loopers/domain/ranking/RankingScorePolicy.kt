package com.loopers.domain.ranking

class RankingScorePolicy {
    fun calculateIncrement(
        signalType: RankingSignalType,
        quantity: Int = 1,
    ): Double =
        when (signalType) {
            RankingSignalType.VIEW -> VIEW_WEIGHT
            RankingSignalType.LIKE -> LIKE_WEIGHT
            RankingSignalType.ORDER -> ORDER_WEIGHT * quantity
        }

    companion object {
        const val VIEW_WEIGHT = 0.1
        const val LIKE_WEIGHT = 0.2
        const val ORDER_WEIGHT = 0.7
    }
}
