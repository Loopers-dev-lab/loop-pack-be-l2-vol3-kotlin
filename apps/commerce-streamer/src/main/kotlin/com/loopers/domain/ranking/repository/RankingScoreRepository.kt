package com.loopers.domain.ranking.repository

import java.time.LocalDate

interface RankingScoreRepository {
    fun incrementScore(productId: Long, score: Double, eventId: String, rankingDate: LocalDate)
}
