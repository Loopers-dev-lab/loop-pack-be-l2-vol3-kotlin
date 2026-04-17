package com.loopers.domain.ranking

import java.time.LocalDate

/**
 * 주간/월간 랭킹을 Materialized View 테이블에서 조회하는 도메인 Repository.
 *
 * - 일간 랭킹은 기존 [RankingRepository] (Redis ZSET) 경로를 그대로 사용한다.
 * - 주간/월간 은 배치가 적재한 `mv_product_rank_*` 테이블에서 조회한다.
 */
interface PeriodicRankingRepository {
    fun findTopWeekly(periodStart: LocalDate, offset: Long, limit: Long): List<RankedProduct>

    fun countWeekly(periodStart: LocalDate): Long

    fun findTopMonthly(yearMonthVal: String, offset: Long, limit: Long): List<RankedProduct>

    fun countMonthly(yearMonthVal: String): Long
}
