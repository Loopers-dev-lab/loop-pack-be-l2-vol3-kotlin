package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface RankingEventJpaRepository : JpaRepository<RankingEvent, Long> {
    @Query(
        """
        SELECT re.productId, FUNCTION('DATE_FORMAT', re.createdAt, '%Y%m%d'), SUM(re.score), SUM(re.rawCount)
        FROM RankingEvent re
        WHERE re.aggregated = false
        GROUP BY re.productId, FUNCTION('DATE_FORMAT', re.createdAt, '%Y%m%d')
        """,
    )
    fun aggregateUnaggregated(): List<Array<Any>>

    @Modifying
    @Query("UPDATE RankingEvent re SET re.aggregated = true WHERE re.aggregated = false")
    fun markAllAggregated()

    @Modifying
    @Query("DELETE FROM RankingEvent re WHERE re.aggregated = true AND re.createdAt < :before")
    fun deleteAggregatedBefore(before: ZonedDateTime)
}
