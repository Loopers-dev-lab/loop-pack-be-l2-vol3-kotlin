package com.loopers.infrastructure.ranking.mv

import com.loopers.domain.ranking.mv.WeeklyProductRankModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface WeeklyProductRankJpaRepository : JpaRepository<WeeklyProductRankModel, Long> {
    fun findByPeriodStartOrderByRankPositionAsc(
        periodStart: LocalDate,
        pageable: Pageable,
    ): List<WeeklyProductRankModel>

    fun countByPeriodStart(periodStart: LocalDate): Long
}
