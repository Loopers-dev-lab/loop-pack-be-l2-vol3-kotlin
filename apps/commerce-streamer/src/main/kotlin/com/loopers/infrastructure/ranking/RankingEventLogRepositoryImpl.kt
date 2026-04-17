package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEventLog
import com.loopers.domain.ranking.RankingEventLogRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingEventLogRepositoryImpl(
    private val rankingEventLogJpaRepository: RankingEventLogJpaRepository,
) : RankingEventLogRepository {

    override fun save(log: RankingEventLog): RankingEventLog {
        return rankingEventLogJpaRepository.save(log)
    }

    override fun findByOccurredDate(date: LocalDate): List<RankingEventLog> {
        return rankingEventLogJpaRepository.findByOccurredDate(date)
    }
}
