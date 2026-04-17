package com.loopers.infrastructure.ranking

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductRankMonthlyJpaRepository : JpaRepository<ProductRankMonthlyEntity, Long> {

    fun findByRankingDateOrderByRankingAsc(
        rankingDate: LocalDate,
        pageable: Pageable,
    ): List<ProductRankMonthlyEntity>

    fun countByRankingDate(rankingDate: LocalDate): Long
}
