package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEventLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RankingEventLogJpaRepository : JpaRepository<RankingEventLog, Long> {
    fun findByOccurredDate(date: LocalDate): List<RankingEventLog>
}
