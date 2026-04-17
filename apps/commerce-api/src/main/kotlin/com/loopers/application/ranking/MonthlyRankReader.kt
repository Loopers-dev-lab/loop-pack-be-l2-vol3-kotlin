package com.loopers.application.ranking

interface MonthlyRankReader {

    fun findLatestRanks(offset: Long, limit: Int): List<MonthlyRankView>

    fun countLatest(): Long

    fun findLatestRankOfProduct(productId: Long): Int?
}
