package com.loopers.domain.ranking

import java.time.LocalDate

/**
 * 주간 랭킹 MV 조회 전용 인터페이스.
 *
 * 기존 `ProductRankingQueryRepository`(Redis 일간용)와는 별도 인터페이스로 둬서
 * 동일 타입에 대한 autowiring 충돌을 피한다. UseCase는 period별로 별도 인터페이스를 직접 주입받는다.
 */
interface WeeklyRankQueryRepository {
    fun getTopRanked(date: LocalDate, offset: Long, count: Long): List<RankedProduct>

    fun getTotalCount(date: LocalDate): Long
}
