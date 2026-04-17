package com.loopers.batch.job.ranking.param

import com.loopers.domain.ranking.RankingPeriodType
import java.time.LocalDate

data class RankingJobParameter(
    val requestDate: LocalDate,
    val periodType: RankingPeriodType,
) {
    val periodStartDate: LocalDate
        get() = periodType.periodStartDate(requestDate)

    val periodEndDate: LocalDate
        get() = periodType.periodEndDate(requestDate)
}
