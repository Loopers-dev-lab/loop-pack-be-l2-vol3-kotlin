package com.loopers.application.ranking

import java.time.LocalDate

interface RankingStrategy {
    fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo>
}
