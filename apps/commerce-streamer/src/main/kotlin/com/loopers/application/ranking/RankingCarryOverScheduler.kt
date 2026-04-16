package com.loopers.application.ranking

import com.loopers.common.DateUtils
import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RankingCarryOverScheduler(
    private val redisZSetTemplate: RedisZSetTemplate,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 50 23 * * ?")
    fun carryOver() {
        val today = DateUtils.todayKst()
        val tomorrow = DateUtils.tomorrowKst()
        val todayKey = RankingKeyGenerator.dailyKey(today)
        val tomorrowKey = RankingKeyGenerator.dailyKey(tomorrow)

        try {
            val count = redisZSetTemplate.unionStoreWithWeight(tomorrowKey, todayKey, rankingProperties.carryOverWeight)
            redisZSetTemplate.setTtlIfAbsent(tomorrowKey, Duration.ofDays(rankingProperties.ttlDays))
            log.info("랭킹 Carry-Over 완료 [from={}, to={}, count={}]", today, tomorrow, count)
        } catch (e: Exception) {
            log.error("랭킹 Carry-Over 실패 [from={}, to={}]", today, tomorrow, e)
        }
    }
}
