package com.loopers.application.ranking

import com.loopers.domain.ranking.Period

data class RankingResult(
    val period: Period,
    val periodStart: String,
    val periodEnd: String,
    val items: List<RankingInfo>,
)
