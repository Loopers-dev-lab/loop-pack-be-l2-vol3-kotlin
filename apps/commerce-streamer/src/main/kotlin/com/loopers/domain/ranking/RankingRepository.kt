package com.loopers.domain.ranking

/**
 * 랭킹 ZSET 쓰기 Repository 인터페이스 (Domain Layer).
 *
 * Infrastructure에서 Redis 기반으로 구현한다.
 * 일간(daily) + 시간(hourly) 두 윈도우를 모두 지원한다.
 */
interface RankingRepository {

    /**
     * 일간 + 시간 랭킹 ZSET에 점수를 동시에 갱신한다.
     *
     * 두 키를 같은 파이프라인으로 처리하여 단일 왕복으로 완료한다.
     *
     * @param dateKey 일간 키 (yyyyMMdd)
     * @param hourKey 시간 키 (yyyyMMddHH)
     * @param scores productId → score 매핑
     */
    fun updateScores(dateKey: String, hourKey: String, scores: Map<Long, Double>)

    /**
     * 전일 랭킹을 다음 날 키로 복사한다 (Score Carry-Over).
     *
     * 콜드 스타트 완화를 위해 사용한다.
     * 전일 점수에 `weight` 배율을 곱한 값으로 복사된다.
     *
     * @param fromDateKey 소스 날짜 키 (yyyyMMdd)
     * @param toDateKey 대상 날짜 키 (yyyyMMdd)
     * @param weight 감쇠 가중치 (0.0 ~ 1.0)
     */
    fun carryOverScores(fromDateKey: String, toDateKey: String, weight: Double)
}
