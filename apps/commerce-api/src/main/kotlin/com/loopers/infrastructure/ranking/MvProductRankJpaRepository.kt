package com.loopers.infrastructure.ranking

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeeklyJpaModel, MvProductRankId> {
    fun findByPeriodKeyOrderByRankValueAsc(periodKey: String, pageable: Pageable): List<MvProductRankWeeklyJpaModel>
    fun countByPeriodKey(periodKey: String): Long
}

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthlyJpaModel, MvProductRankId> {
    fun findByPeriodKeyOrderByRankValueAsc(periodKey: String, pageable: Pageable): List<MvProductRankMonthlyJpaModel>
    fun countByPeriodKey(periodKey: String): Long
}
