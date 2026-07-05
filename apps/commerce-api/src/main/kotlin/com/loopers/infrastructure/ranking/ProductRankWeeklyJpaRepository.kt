package com.loopers.infrastructure.ranking

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductRankWeeklyJpaRepository : JpaRepository<ProductRankWeeklyEntity, Long> {

    fun findByRankingDateOrderByRankingAsc(
        rankingDate: LocalDate,
        pageable: Pageable,
    ): List<ProductRankWeeklyEntity>

    fun countByRankingDate(rankingDate: LocalDate): Long
}
