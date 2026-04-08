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
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 30_000)
    fun flush() {
        val counts = viewCountBuffer.drainAll()
        if (counts.isEmpty()) return

        try {
            val key = RankingKeyGenerator.todayKey()
            counts.forEach { (productId, count) ->
                redisZSetTemplate.incrementScore(key, productId.toString(), rankingProperties.weight.view * count)
            }
            redisZSetTemplate.setTtlIfAbsent(key, Duration.ofDays(rankingProperties.ttlDays))
            log.info("VIEW 점수 flush [상품수={}, 총조회수={}]", counts.size, counts.values.sum())
        } catch (e: Exception) {
            log.error("VIEW 점수 flush 실패 [상품수={}]", counts.size, e)
        }
    }
}
