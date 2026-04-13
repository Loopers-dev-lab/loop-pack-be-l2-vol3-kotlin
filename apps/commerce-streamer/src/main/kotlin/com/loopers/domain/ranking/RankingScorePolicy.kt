package com.loopers.domain.ranking

import kotlin.math.ln

class RankingScorePolicy {
    /**
     * VIEW/LIKE 등 단순 시그널의 가중치 반환.
     * ORDER는 orderAmount 기반이므로 [calculateOrderIncrement] 사용.
     */
    fun calculateIncrement(
        signalType: RankingSignalType,
    ): Double =
        when (signalType) {
            RankingSignalType.VIEW -> VIEW_WEIGHT
            RankingSignalType.LIKE -> LIKE_WEIGHT
            RankingSignalType.ORDER -> throw IllegalArgumentException(
                "ORDER signal requires orderAmount. Use calculateOrderIncrement() instead.",
            )
        }

    fun calculateOrderIncrement(orderAmount: Long): Double {
        if (orderAmount <= 0) return 0.0
        return ORDER_WEIGHT * ln(orderAmount.toDouble())
    }

    companion object {
        const val VIEW_WEIGHT = 0.1
        const val LIKE_WEIGHT = 0.2
        const val ORDER_WEIGHT = 0.7
    }
}
