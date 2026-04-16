package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthly, Long> {

    @Query("SELECT MAX(m.version) FROM MvProductRankMonthly m WHERE m.yearMonth = :yearMonth")
    fun findMaxVersionByYearMonth(yearMonth: String): Int?

    @Modifying
    @Query("DELETE FROM MvProductRankMonthly m WHERE m.yearMonth = :yearMonth AND m.version < :version")
    fun deleteByYearMonthAndVersionLessThan(yearMonth: String, version: Int)
}
