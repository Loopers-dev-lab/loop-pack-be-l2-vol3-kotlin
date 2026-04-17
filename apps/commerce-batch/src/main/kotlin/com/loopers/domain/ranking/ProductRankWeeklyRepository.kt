package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductRankWeeklyRepository {
    fun batchInsert(entities: List<ProductRankResult>, periodStartDate: LocalDate, periodEndDate: LocalDate)
    fun deleteByPeriodStartDate(periodStartDate: LocalDate)
}
