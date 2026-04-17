package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingPeriod
import java.time.LocalDate

data class GetRankingCriteria(
    val date: LocalDate,
    val page: Int,
    val size: Int,
    val period: RankingPeriod = RankingPeriod.DAILY,
)
