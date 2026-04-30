package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthly
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthly, Long> {

    @Query(
        "SELECT m FROM MvProductRankMonthly m " +
            "WHERE m.yearMonth = :yearMonth AND m.version = (" +
            "  SELECT MAX(m2.version) FROM MvProductRankMonthly m2 WHERE m2.yearMonth = :yearMonth" +
            ") ORDER BY m.rank ASC",
    )
    fun findByYearMonthLatestVersion(yearMonth: String, pageable: Pageable): List<MvProductRankMonthly>

    @Query(
        "SELECT COUNT(m) FROM MvProductRankMonthly m " +
            "WHERE m.yearMonth = :yearMonth AND m.version = (" +
            "  SELECT MAX(m2.version) FROM MvProductRankMonthly m2 WHERE m2.yearMonth = :yearMonth" +
            ")",
    )
    fun countByYearMonthLatestVersion(yearMonth: String): Long
}
