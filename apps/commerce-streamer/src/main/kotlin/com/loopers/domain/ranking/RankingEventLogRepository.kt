package com.loopers.domain.ranking

import java.time.LocalDate

interface RankingEventLogRepository {
    fun save(log: RankingEventLog): RankingEventLog
    fun findByOccurredDate(date: LocalDate): List<RankingEventLog>
}
