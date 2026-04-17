package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.WeeklyRank
import com.loopers.domain.ranking.WeeklyRankId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface WeeklyRankJpaRepository : JpaRepository<WeeklyRank, WeeklyRankId> {

    @Query("SELECT MAX(w.id.weekEnd) FROM WeeklyRank w")
    fun findLatestWeekEnd(): LocalDate?

    fun findAllByIdWeekEndOrderByRankPositionAsc(weekEnd: LocalDate): List<WeeklyRank>
}
