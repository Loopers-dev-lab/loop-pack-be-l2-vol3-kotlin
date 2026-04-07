package com.loopers.config.redis

import java.time.ZoneId

object RedisRankingConstants {
    const val RANKING_KEY_PREFIX = "ranking:all:"
    const val RANKING_TTL_SECONDS = 172_800L
    val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
}
