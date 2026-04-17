package com.loopers.infrastructure.ranking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface MonthlyProductRankJpaRepository : JpaRepository<MonthlyProductRankEntity, Long> {
    /**
     * 특정 (year, month) 범위의 MV 행을 hard delete 한다.
     * 호출 측(Tasklet)의 트랜잭션 경계 내에서 실행된다. 자체 트랜잭션을 시작하지 않는다.
     */
    @Modifying
    fun deleteByYearAndMonth(year: Int, month: Int): Long

    fun findByYearAndMonthOrderByRankNumber(year: Int, month: Int): List<MonthlyProductRankEntity>

    fun countByYearAndMonth(year: Int, month: Int): Long
}
