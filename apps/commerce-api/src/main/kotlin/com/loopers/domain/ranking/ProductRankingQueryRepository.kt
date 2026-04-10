package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductRankingQueryRepository {
    fun getTopRanked(
        date: LocalDate,
        offset: Long,
        count: Long,
    ): List<RankedProduct>

    fun getRank(
        date: LocalDate,
        productId: Long,
    ): Long?

    fun getTotalCount(date: LocalDate): Long
}
