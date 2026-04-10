package com.loopers.domain.ranking

interface RankingEventRepository {
    fun save(event: RankingEvent): RankingEvent
    fun saveAll(events: List<RankingEvent>): List<RankingEvent>
    fun aggregateUnaggregated(): List<AggregatedScore>
    fun markAllAggregated()
    fun deleteAggregatedBefore(before: java.time.ZonedDateTime)
}

data class AggregatedScore(
    val productId: Long,
    val rankingDate: String,
    val totalScore: Double,
    val count: Long,
)
