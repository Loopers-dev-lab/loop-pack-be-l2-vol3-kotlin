package com.loopers.infrastructure.ranking

object RankingScoreFormula {
    const val VIEW_WEIGHT: Double = 0.1
    const val LIKE_WEIGHT: Double = 0.2
    const val SALES_WEIGHT: Double = 1.0

    fun compute(
        viewCount: Long,
        likeCount: Long,
        salesCount: Long,
    ): Double {
        return (viewCount * VIEW_WEIGHT) + (likeCount * LIKE_WEIGHT) + (salesCount * SALES_WEIGHT)
    }
}
