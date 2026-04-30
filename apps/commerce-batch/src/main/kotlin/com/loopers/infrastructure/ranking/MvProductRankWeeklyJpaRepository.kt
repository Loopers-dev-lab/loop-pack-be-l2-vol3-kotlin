package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankWeekly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeekly, Long> {

    @Query("SELECT MAX(m.version) FROM MvProductRankWeekly m WHERE m.yearWeek = :yearWeek")
    fun findMaxVersionByYearWeek(yearWeek: String): Int?

    @Modifying
    @Query("DELETE FROM MvProductRankWeekly m WHERE m.yearWeek = :yearWeek AND m.version < :version")
    fun deleteByYearWeekAndVersionLessThan(yearWeek: String, version: Int)
}
