package com.loopers.batch.job.ranking

// Reader가 product_metrics_daily에서 읽어오는 원시 row
data class ProductMetricsDailyRow(
    val productId: Long,
    val viewCount: Long,
    val salesCount: Long,
    val likeCount: Long,
)

// Processor가 반환하는 점수 기여값 (누적용)
data class RankingScoreContribution(
    val productId: Long,
    val score: Double,
)

data class RankingAggregationItem(
    val productId: Long,
    val totalViewCount: Long,
    val totalSalesCount: Long,
    val totalLikeCount: Long,
    val score: Double = calculateScore(totalViewCount, totalLikeCount, totalSalesCount),
    val rank: Int = 0,
) {
    companion object {
        private fun calculateScore(viewCount: Long, likeCount: Long, salesCount: Long): Double {
            return viewCount * 0.1 + likeCount * 0.2 + (salesCount * 0.7)
        }
    }

    fun withRank(newRank: Int): RankingAggregationItem {
        return this.copy(rank = newRank)
    }
}
