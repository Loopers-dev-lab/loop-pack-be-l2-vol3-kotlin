package com.loopers.infrastructure.ranking

/**
 * 랭킹 가중치 Redis 키 전략.
 *
 * 단일 키 전역 저장. TTL 없음 (영구).
 * commerce-streamer의 RankingWeightsRedisKeys와 동일한 키를 사용한다.
 */
object RankingWeightsRedisKeys {
    const val KEY = "ranking:weights"
}
