package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductRankingRepository {
    fun incrementScore(
        productId: Long,
        increment: Double,
    )

    fun carryOver(
        sourceDate: LocalDate,
        destinationDate: LocalDate,
        weight: Double,
    )

    fun exists(date: LocalDate): Boolean
}
