package com.loopers.infrastructure.mv

import com.loopers.domain.mv.MonthlyProductRankModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MonthlyProductRankJpaRepository : JpaRepository<MonthlyProductRankModel, Long> {
    /**
     * purge Step 에서 재실행(idempotent) 을 보장하기 위해 주어진 월의 모든 행을 하드 삭제한다.
     *
     * @return 삭제된 row 수
     */
    @Modifying
    @Query("DELETE FROM MonthlyProductRankModel m WHERE m.yearMonthVal = :yearMonthVal")
    fun deleteByYearMonthVal(@Param("yearMonthVal") yearMonthVal: String): Int
}
