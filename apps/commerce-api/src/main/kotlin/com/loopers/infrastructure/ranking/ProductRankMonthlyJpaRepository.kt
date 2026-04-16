package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankMonthly
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRankMonthlyJpaRepository : JpaRepository<ProductRankMonthly, Long> {
    fun findByPeriodDateOrderByRankingRankAsc(periodDate: String, pageable: Pageable): List<ProductRankMonthly>
    fun countByPeriodDate(periodDate: String): Long
}
