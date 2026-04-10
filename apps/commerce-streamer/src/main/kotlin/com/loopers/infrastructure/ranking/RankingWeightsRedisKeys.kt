package com.loopers.infrastructure.ranking

/**
 * 랭킹 가중치 Redis 키 전략.
 *
 * 단일 키 전역 저장. 운영자가 Admin API로 갱신한다.
 * TTL 없음 (영구 저장).
 */
object RankingWeightsRedisKeys {
    const val KEY = "ranking:weights"
}
