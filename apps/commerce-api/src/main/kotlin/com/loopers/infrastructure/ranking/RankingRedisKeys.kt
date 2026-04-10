package com.loopers.infrastructure.ranking

/**
 * 랭킹 Redis 키 전략.
 *
 * 일간: ranking:all:{yyyyMMdd}
 * 시간: ranking:all:hourly:{yyyyMMddHH}
 */
object RankingRedisKeys {

    private const val DAILY_PREFIX = "ranking:all"
    private const val HOURLY_PREFIX = "ranking:all:hourly"

    fun dailyKey(dateKey: String): String = "$DAILY_PREFIX:$dateKey"

    fun hourlyKey(hourKey: String): String = "$HOURLY_PREFIX:$hourKey"
}
