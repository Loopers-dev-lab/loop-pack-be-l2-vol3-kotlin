package com.loopers.domain.ranking

import java.time.LocalDate

interface RankingRepository {
    fun incrementScore(date: LocalDate, productId: Long, score: Double)
}
