package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisRankingConstants
import com.loopers.domain.ranking.model.RankingEntry
import com.loopers.domain.ranking.model.RankingFetchResult
import com.loopers.domain.ranking.repository.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class RedisRankingRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getTopN(date: LocalDate, offset: Int, limit: Int): RankingFetchResult {
        val key = rankingKey(date)
        // Lua 스크립트로 score > 0 (exclusive 0) 조건 조회

        @Suppress("UNCHECKED_CAST")
        val raw = redisTemplate.execute(
            GET_TOP_N_SCRIPT,
            listOf(key),
            offset.toString(),
            limit.toString(),
        ) as? List<*> ?: return RankingFetchResult(entries = emptyList(), rawFetchCount = 0)

        // ZREVRANGEBYSCORE WITHSCORES 결과: [member, score, member, score, ...]
        val rawFetchCount = raw.size / 2
        val entries = raw.chunked(2).mapNotNull { pair ->
            val productId = pair.getOrNull(0)?.toString()?.toLongOrNull() ?: run {
                log.warn("Redis 랭킹 파싱 실패 [key={}, member={}]", key, pair.getOrNull(0))
                return@mapNotNull null
            }
            val score = pair.getOrNull(1)?.toString()?.toDoubleOrNull() ?: return@mapNotNull null
            RankingEntry(productId = productId, score = score)
        }
        return RankingFetchResult(entries = entries, rawFetchCount = rawFetchCount)
    }

    override fun getRank(date: LocalDate, productId: Long): Int? {
        val key = rankingKey(date)
        return try {
            val rank = redisTemplate.opsForZSet().reverseRank(key, productId.toString())
                ?: return null
            val score = redisTemplate.opsForZSet().score(key, productId.toString())
                ?: return null
            if (score <= 0) return null
            (rank + 1).toInt()
        } catch (e: Exception) {
            log.warn("Redis 순위 조회 실패 [key={}, productId={}]: {}", key, productId, e.message)
            null
        }
    }

    private fun rankingKey(date: LocalDate): String =
        "${RedisRankingConstants.RANKING_KEY_PREFIX}${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"

    companion object {
        /**
         * score > 0 항목만 조회하는 Lua 스크립트.
         * Redis의 exclusive bound 문법 '(0'을 사용하여 score=0 이하를 명시적으로 제외한다.
         */
        private val GET_TOP_N_SCRIPT = DefaultRedisScript<List<*>>(
            """
            return redis.call('ZREVRANGEBYSCORE', KEYS[1], '+inf', '(0', 'WITHSCORES', 'LIMIT', ARGV[1], ARGV[2])
            """.trimIndent(),
            List::class.java,
        )
    }
}
