package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.AggregatedScore
import com.loopers.domain.ranking.RankingEvent
import com.loopers.domain.ranking.RankingEventRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class RankingEventRepositoryImpl(
    private val rankingEventJpaRepository: RankingEventJpaRepository,
) : RankingEventRepository {

    override fun save(event: RankingEvent): RankingEvent {
        return rankingEventJpaRepository.save(event)
    }

    override fun saveAll(events: List<RankingEvent>): List<RankingEvent> {
        return rankingEventJpaRepository.saveAll(events)
    }

    override fun aggregateUnaggregated(): List<AggregatedScore> {
        return rankingEventJpaRepository.aggregateUnaggregated().map { row ->
            AggregatedScore(
                productId = row[0] as Long,
                rankingDate = (row[1] as String).replace("-", ""),
                totalScore = (row[2] as Number).toDouble(),
                count = (row[3] as Number).toLong(),
            )
        }
    }

    override fun markAllAggregated() {
        rankingEventJpaRepository.markAllAggregated()
    }

    override fun deleteAggregatedBefore(before: ZonedDateTime) {
        rankingEventJpaRepository.deleteAggregatedBefore(before)
    }
}
