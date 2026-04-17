package com.loopers.application.ranking

interface WeeklyRankReader {

    fun findLatestRanks(offset: Long, limit: Int): List<WeeklyRankView>

    fun countLatest(): Long

    fun findLatestRankOfProduct(productId: Long): Int?
}
