package com.loopers.application.ranking

import java.time.LocalDate

data class GetRankingCriteria(
    val date: LocalDate,
    val page: Int,
    val size: Int,
)
