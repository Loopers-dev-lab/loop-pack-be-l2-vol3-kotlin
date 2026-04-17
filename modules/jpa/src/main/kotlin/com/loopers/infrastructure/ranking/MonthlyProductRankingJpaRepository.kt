package com.loopers.infrastructure.ranking

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MonthlyProductRankingJpaRepository : JpaRepository<MonthlyProductRankingEntity, Long> {
    fun deleteAllByMonthStartDate(monthStartDate: LocalDate)

    fun findAllByMonthStartDateOrderByRankingAsc(monthStartDate: LocalDate, pageable: Pageable): Page<MonthlyProductRankingEntity>
}
