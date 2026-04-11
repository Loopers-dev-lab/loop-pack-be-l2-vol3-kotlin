package com.loopers.domain.ranking

/**
 * 랭킹 ZSET 읽기 Repository 인터페이스 (Domain Layer).
 *
 * Infrastructure에서 Redis 기반으로 구현한다.
 * 일간(daily) / 시간(hourly) 윈도우를 모두 지원한다.
 */
interface RankingRepository {

    /**
     * 지정 윈도우의 Top-N을 score 내림차순으로 조회한다.
     */
    fun getTopRankings(
        window: RankingWindow,
        windowKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry>

    /**
     * 지정 윈도우에서 특정 상품의 순위를 조회한다 (0-based, 없으면 null).
     */
    fun getRank(window: RankingWindow, windowKey: String, productId: Long): Long?

    /**
     * 지정 윈도우에서 특정 상품의 score를 조회한다 (없으면 null).
     */
    fun getScore(window: RankingWindow, windowKey: String, productId: Long): Double?

    /**
     * 지정 윈도우의 전체 member 수를 조회한다.
     */
    fun getTotalCount(window: RankingWindow, windowKey: String): Long
}
