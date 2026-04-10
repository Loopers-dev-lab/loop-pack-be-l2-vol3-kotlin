package com.loopers.batch.job.ranking

data class RankingScore(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
) {
    companion object {
        private const val VIEW_WEIGHT = 0.1
        private const val LIKE_WEIGHT = 0.2
        private const val ORDER_WEIGHT = 0.7
    }

    fun calculateScore(): Double {
        return viewCount * VIEW_WEIGHT + likeCount * LIKE_WEIGHT + salesCount * ORDER_WEIGHT
    }
}
