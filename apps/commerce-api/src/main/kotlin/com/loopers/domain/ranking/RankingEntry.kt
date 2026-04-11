package com.loopers.domain.ranking

/**
 * ZSET에서 조회한 원시 랭킹 엔트리.
 */
data class RankingEntry(
    val productId: Long,
    val score: Double,
)
