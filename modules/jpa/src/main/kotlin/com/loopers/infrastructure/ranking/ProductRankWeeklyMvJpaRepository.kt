package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankWeeklyMvModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductRankWeeklyMvJpaRepository : JpaRepository<ProductRankWeeklyMvModel, Long> {
    fun findAllByPeriodStartDate(periodStartDate: LocalDate, pageable: Pageable): Page<ProductRankWeeklyMvModel>

    @Query(
        """
        select max(snapshot.periodStartDate)
        from ProductRankWeeklyMvModel snapshot
        """,
    )
    fun findLatestPeriodStartDate(): LocalDate?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from ProductRankWeeklyMvModel snapshot
        where snapshot.periodStartDate = :periodStartDate
          and snapshot.periodEndDate = :periodEndDate
        """,
    )
    fun deleteByPeriod(
        @Param("periodStartDate") periodStartDate: LocalDate,
        @Param("periodEndDate") periodEndDate: LocalDate,
    )
}
