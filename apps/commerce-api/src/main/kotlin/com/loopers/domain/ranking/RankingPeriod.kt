package com.loopers.domain.ranking

/**
 * 랭킹 조회 기간.
 *
 * - [DAILY]: 실시간 일간 랭킹 (Redis ZSET 조회)
 * - [WEEKLY]: 주간 TOP N (배치 적재 MV 테이블 조회, ISO 주 월~일)
 * - [MONTHLY]: 월간 TOP N (배치 적재 MV 테이블 조회, 1일~말일)
 */
enum class RankingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}
