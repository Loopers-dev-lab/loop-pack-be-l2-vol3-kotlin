package com.loopers.batch.infrastructure.ranking

import com.loopers.batch.domain.ranking.MvProductRankMonthly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthly, Long> {
    @Modifying
    @Query("DELETE FROM MvProductRankMonthly m WHERE m.yearMonth = :yearMonth")
    fun deleteByYearMonth(@Param("yearMonth") yearMonth: String)
}
