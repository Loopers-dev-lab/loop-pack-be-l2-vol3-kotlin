package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankMonthlyMvModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductRankMonthlyMvJpaRepository : JpaRepository<ProductRankMonthlyMvModel, Long> {
    fun findAllByPeriodStartDate(periodStartDate: LocalDate, pageable: Pageable): Page<ProductRankMonthlyMvModel>

    @Query(
        """
        select max(snapshot.periodStartDate)
        from ProductRankMonthlyMvModel snapshot
        """,
    )
    fun findLatestPeriodStartDate(): LocalDate?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from ProductRankMonthlyMvModel snapshot
        where snapshot.periodStartDate = :periodStartDate
          and snapshot.periodEndDate = :periodEndDate
        """,
    )
    fun deleteByPeriod(
        @Param("periodStartDate") periodStartDate: LocalDate,
        @Param("periodEndDate") periodEndDate: LocalDate,
    )
}
