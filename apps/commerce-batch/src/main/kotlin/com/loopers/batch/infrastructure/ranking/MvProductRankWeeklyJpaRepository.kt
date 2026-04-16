package com.loopers.batch.infrastructure.ranking

import com.loopers.batch.domain.ranking.MvProductRankWeekly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeekly, Long> {
    @Modifying
    @Query("DELETE FROM MvProductRankWeekly m WHERE m.yearWeek = :yearWeek")
    fun deleteByYearWeek(@Param("yearWeek") yearWeek: String)
}
