package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional

interface WeeklyProductRankJpaRepository : JpaRepository<WeeklyProductRankEntity, Long> {
    /**
     * 특정 (year, week) 범위의 MV 행을 hard delete 한다.
     * 멱등성 재실행을 위한 용도. Tasklet Step 내에서 호출되어 외부 transactionManager에 경계가 맡겨진다.
     */
    @Modifying
    @Transactional
    fun deleteByYearAndWeek(year: Int, week: Int): Long

    fun findByYearAndWeekOrderByRankNumber(year: Int, week: Int): List<WeeklyProductRankEntity>

    fun countByYearAndWeek(year: Int, week: Int): Long
}
