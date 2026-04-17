package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRank
import com.loopers.domain.ranking.MonthlyRankId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MonthlyRankJpaRepository : JpaRepository<MonthlyRank, MonthlyRankId> {

    @Query("SELECT MAX(m.id.yearMonth) FROM MonthlyRank m")
    fun findLatestYearMonth(): String?

    @Query("SELECT m FROM MonthlyRank m WHERE m.id.yearMonth = :yearMonth ORDER BY m.rankPosition ASC")
    fun findAllByIdYearMonthOrderByRankPositionAsc(yearMonth: String): List<MonthlyRank>
}
