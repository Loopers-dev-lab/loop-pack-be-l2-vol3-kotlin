package com.loopers.infrastructure.ranking.mv

import com.loopers.domain.ranking.mv.MonthlyProductRankModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyProductRankJpaRepository : JpaRepository<MonthlyProductRankModel, Long> {
    fun findByYearMonthValOrderByRankPositionAsc(
        yearMonthVal: String,
        pageable: Pageable,
    ): List<MonthlyProductRankModel>

    fun countByYearMonthVal(yearMonthVal: String): Long
}
