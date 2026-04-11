package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingWindow

data class GetRankingsCriteria(
    val window: RankingWindow,
    val windowKey: String,
    val page: Int,
    val size: Int,
)
