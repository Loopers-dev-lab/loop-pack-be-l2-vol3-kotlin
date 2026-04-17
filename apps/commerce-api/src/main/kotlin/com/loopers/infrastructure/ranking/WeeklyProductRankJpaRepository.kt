package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository

interface WeeklyProductRankJpaRepository : JpaRepository<WeeklyProductRankEntity, Long> {
    fun findByYearAndWeekOrderByRankNumber(year: Int, week: Int): List<WeeklyProductRankEntity>

    fun countByYearAndWeek(year: Int, week: Int): Long
}
