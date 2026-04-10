package com.loopers.domain.ranking

interface RankingRepository {
    fun getTopN(key: String, offset: Long, count: Long): List<RankingEntry>
    fun getRank(key: String, productId: Long): Long?
    fun getScore(key: String, productId: Long): Double?
    fun getTotalCount(key: String): Long
}

data class RankingEntry(
    val productId: Long,
    val score: Double,
)
