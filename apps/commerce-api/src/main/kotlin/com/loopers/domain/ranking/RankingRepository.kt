package com.loopers.domain.ranking

import java.time.LocalDate

interface RankingRepository {

    fun getTopRankings(date: LocalDate, start: Long, end: Long): List<ProductRankingScore>

    fun getProductRank(productId: Long, date: LocalDate): Long?

    fun getProductScore(productId: Long, date: LocalDate): Double?

    fun getTotalCount(date: LocalDate): Long
}

data class ProductRankingScore(
    val productId: Long,
    val score: Double,
)
