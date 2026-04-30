package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingAggregateTemp
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RankingAggregateTempJpaRepository : JpaRepository<RankingAggregateTemp, Long> {

    @Query(
        "SELECT r FROM RankingAggregateTemp r " +
            "WHERE r.periodType = :periodType AND r.periodKey = :periodKey " +
            "ORDER BY r.score DESC",
    )
    fun findTopByPeriod(periodType: String, periodKey: String, pageable: Pageable): List<RankingAggregateTemp>

    @Modifying
    @Query("DELETE FROM RankingAggregateTemp r WHERE r.periodType = :periodType AND r.periodKey = :periodKey")
    fun deleteByPeriod(periodType: String, periodKey: String)
}
