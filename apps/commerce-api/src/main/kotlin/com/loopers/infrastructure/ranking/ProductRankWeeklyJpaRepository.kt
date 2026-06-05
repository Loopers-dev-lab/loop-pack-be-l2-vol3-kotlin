package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankWeekly
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRankWeeklyJpaRepository : JpaRepository<ProductRankWeekly, Long> {
    fun findByPeriodDateOrderByRankingRankAsc(periodDate: String, pageable: Pageable): List<ProductRankWeekly>
    fun countByPeriodDate(periodDate: String): Long
}
