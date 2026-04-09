package com.loopers.application.ranking

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RankingScoreFlushScheduler(
    private val rankingScoreBuffer: RankingScoreBuffer,
    private val redisZSetTemplate: RedisZSetTemplate,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 5_000)
    fun flush() {
        val scores = rankingScoreBuffer.drainAll()
        if (scores.isEmpty()) return

        try {
            val key = RankingKeyGenerator.todayKey()
            scores.forEach { (productId, score) ->
                redisZSetTemplate.incrementScore(key, productId.toString(), score)
            }
            redisZSetTemplate.setTtlIfAbsent(key, Duration.ofDays(rankingProperties.ttlDays))
            log.debug("랭킹 점수 flush [상품수={}, 총점수={}]", scores.size, scores.values.sum())
        } catch (e: Exception) {
            log.error("랭킹 점수 flush 실패 [상품수={}]", scores.size, e)
        }
    }
}
