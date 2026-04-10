package com.loopers.batch.job.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DB 집계값 기반 점수를 계산하고, Redis 현재 점수와 비교하여 불일치 항목만 통과시킨다.
 */
@Component
class RankingReconciliationProcessor(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : ItemProcessor<RankingScore, RankingScore> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override fun process(item: RankingScore): RankingScore? {
        val key = RedisKeys.rankingKey(LocalDate.now().format(dateFormatter))
        val redisScore = masterRedisTemplate.opsForZSet().score(key, item.productId.toString())
        val dbScore = item.calculateScore()

        if (redisScore != null && kotlin.math.abs(redisScore - dbScore) < 0.001) {
            return null
        }

        log.info(
            "[RankingReconciliation] 불일치 감지: productId={}, Redis={}, DB={}",
            item.productId,
            redisScore,
            dbScore,
        )
        return item
    }
}
