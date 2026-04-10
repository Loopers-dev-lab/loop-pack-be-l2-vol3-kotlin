package com.loopers.batch.job.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 불일치 항목의 Redis 랭킹 점수를 DB 기준으로 보정한다.
 */
@Component
class RankingReconciliationWriter(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : ItemWriter<RankingScore> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    companion object {
        private const val TTL_SECONDS = 2 * 24 * 60 * 60L
    }

    override fun write(chunk: Chunk<out RankingScore>) {
        val key = RedisKeys.rankingKey(LocalDate.now().format(dateFormatter))
        val zSetOps = masterRedisTemplate.opsForZSet()

        chunk.items.forEach { item ->
            zSetOps.add(key, item.productId.toString(), item.calculateScore())
        }

        // 키에 TTL이 없으면 설정
        val ttl = masterRedisTemplate.getExpire(key)
        if (ttl == null || ttl == -1L) {
            masterRedisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS)
        }

        log.info("[RankingReconciliation] 보정 완료: {}건", chunk.items.size)
    }
}
