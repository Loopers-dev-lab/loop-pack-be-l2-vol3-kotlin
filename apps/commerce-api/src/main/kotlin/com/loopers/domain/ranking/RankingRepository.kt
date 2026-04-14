package com.loopers.domain.ranking

interface RankingRepository {
    fun getTopNWithScores(key: String, start: Long, end: Long): List<RankingEntry>
    fun getTotalCount(key: String): Long
    fun getRank(key: String, productId: Long): Long?
}

data class RankingEntry(
    val productId: Long,
    val score: Double,
)
