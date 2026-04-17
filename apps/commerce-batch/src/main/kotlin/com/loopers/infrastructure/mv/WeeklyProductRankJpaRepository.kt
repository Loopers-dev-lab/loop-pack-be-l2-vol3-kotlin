package com.loopers.infrastructure.mv

import com.loopers.domain.mv.WeeklyProductRankModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface WeeklyProductRankJpaRepository : JpaRepository<WeeklyProductRankModel, Long> {
    /**
     * purge Step 에서 재실행(idempotent) 을 보장하기 위해 주어진 주의 모든 행을 하드 삭제한다.
     *
     * @return 삭제된 row 수
     */
    @Modifying
    @Query("DELETE FROM WeeklyProductRankModel w WHERE w.periodStart = :periodStart")
    fun deleteByPeriodStart(@Param("periodStart") periodStart: LocalDate): Int
}
