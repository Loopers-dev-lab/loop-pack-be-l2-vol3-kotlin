package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankWeekly
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeekly, Long> {

    @Query(
        "SELECT m FROM MvProductRankWeekly m " +
            "WHERE m.yearWeek = :yearWeek AND m.version = (" +
            "  SELECT MAX(m2.version) FROM MvProductRankWeekly m2 WHERE m2.yearWeek = :yearWeek" +
            ") ORDER BY m.rank ASC",
    )
    fun findByYearWeekLatestVersion(yearWeek: String, pageable: Pageable): List<MvProductRankWeekly>

    @Query(
        "SELECT COUNT(m) FROM MvProductRankWeekly m " +
            "WHERE m.yearWeek = :yearWeek AND m.version = (" +
            "  SELECT MAX(m2.version) FROM MvProductRankWeekly m2 WHERE m2.yearWeek = :yearWeek" +
            ")",
    )
    fun countByYearWeekLatestVersion(yearWeek: String): Long
}
