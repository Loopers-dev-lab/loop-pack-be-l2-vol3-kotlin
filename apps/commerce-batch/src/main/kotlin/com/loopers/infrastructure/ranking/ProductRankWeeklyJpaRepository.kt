package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ProductRankWeeklyJpaRepository : JpaRepository<ProductRankWeeklyEntity, Long> {

    @Modifying
    @Query("DELETE FROM ProductRankWeeklyEntity e WHERE e.rankingDate = :rankingDate")
    fun deleteByRankingDate(rankingDate: LocalDate)
}
