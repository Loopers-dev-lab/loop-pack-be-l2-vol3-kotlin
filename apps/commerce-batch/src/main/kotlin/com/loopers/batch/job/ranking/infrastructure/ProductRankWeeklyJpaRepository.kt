package com.loopers.batch.job.ranking.infrastructure

import com.loopers.batch.job.ranking.domain.ProductRankWeekly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductRankWeeklyJpaRepository : JpaRepository<ProductRankWeekly, Long> {
    @Modifying
    @Query("DELETE FROM ProductRankWeekly p WHERE p.periodDate = :periodDate")
    fun deleteByPeriodDate(periodDate: String)
}
