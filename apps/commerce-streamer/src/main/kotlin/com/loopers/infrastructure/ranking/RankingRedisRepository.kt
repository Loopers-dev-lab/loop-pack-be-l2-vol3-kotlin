package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.ranking.RankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val TTL_SECONDS = 2 * 24 * 60 * 60L // 2일
    }

    private val incrementScript = RedisScript.of<String>(
        ClassPathResource("scripts/ranking_increment.lua"),
        String::class.java,
    )

    override fun incrementScore(date: LocalDate, productId: Long, score: Double) {
        val key = RedisKeys.rankingKey(date.format(DATE_FORMATTER))
        masterRedisTemplate.execute(
            incrementScript,
            listOf(key),
            productId.toString(),
            score.toString(),
            TTL_SECONDS.toString(),
        )
    }
}
