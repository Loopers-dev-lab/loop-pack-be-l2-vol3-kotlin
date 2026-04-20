package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyProductRankJpaRepository : JpaRepository<MonthlyProductRankEntity, Long> {
    fun findByYearAndMonthOrderByRankNumber(year: Int, month: Int): List<MonthlyProductRankEntity>

    fun countByYearAndMonth(year: Int, month: Int): Long
}
