package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ProductRankMonthlyJpaRepository : JpaRepository<ProductRankMonthlyEntity, Long> {

    @Modifying
    @Query("DELETE FROM ProductRankMonthlyEntity e WHERE e.rankingDate = :rankingDate")
    fun deleteByRankingDate(rankingDate: LocalDate)
}
