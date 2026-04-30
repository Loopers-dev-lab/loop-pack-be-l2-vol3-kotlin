package com.loopers.domain.ranking

/**
 * MV 기반 랭킹 조회 Repository (Domain Layer).
 *
 * Infrastructure에서 JPA 기반으로 구현한다.
 * 주간(WEEKLY) / 월간(MONTHLY) 윈도우를 지원한다.
 * 최신 완료 version 기준으로 조회한다 (블루-그린 교체).
 */
interface MvRankingRepository {

    /**
     * 지정 윈도우의 Top-N을 rank 오름차순으로 조회한다.
     */
    fun getTopRankings(
        window: RankingWindow,
        periodKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry>

    /**
     * 지정 윈도우의 전체 member 수를 조회한다.
     */
    fun getTotalCount(window: RankingWindow, periodKey: String): Long
}
