package com.loopers.application.ranking

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ViewCountFlushScheduler(
    private val viewCountBuffer: ViewCountBuffer,
    private val redisZSetTemplate: RedisZSetTemplate,
    private val viewCountPersister: ViewCountPersister,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 30_000)
    fun flush() {
        val counts = viewCountBuffer.drainAll()
        if (counts.isEmpty()) return

        flushToRedis(counts)
        flushToDb(counts)
    }

    private fun flushToRedis(counts: Map<Long, Long>) {
        try {
            val key = RankingKeyGenerator.todayKey()
            counts.forEach { (productId, count) ->
                redisZSetTemplate.incrementScore(key, productId.toString(), rankingProperties.weight.view * count)
            }
            redisZSetTemplate.setTtlIfAbsent(key, Duration.ofDays(rankingProperties.ttlDays))
        } catch (e: Exception) {
            log.error("VIEW Redis flush 실패 [상품수={}]", counts.size, e)
        }
    }

    private fun flushToDb(counts: Map<Long, Long>) {
        try {
            viewCountPersister.incrementViewCounts(counts)
        } catch (e: Exception) {
            log.error("VIEW DB flush 실패 [상품수={}]", counts.size, e)
        }
    }
}
