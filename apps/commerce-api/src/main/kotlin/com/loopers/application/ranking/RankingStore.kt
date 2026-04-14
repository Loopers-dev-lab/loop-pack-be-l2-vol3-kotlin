package com.loopers.application.ranking

import java.time.LocalDate

interface RankingStore {
    fun getTopRankings(date: LocalDate, offset: Long, limit: Long): List<RankingEntry>
    fun getProductRank(date: LocalDate, productId: Long): Long?
    fun getProductScore(date: LocalDate, productId: Long): Double?
    fun size(date: LocalDate): Long
}

data class RankingEntry(
    val productId: Long,
    val score: Double,
)
