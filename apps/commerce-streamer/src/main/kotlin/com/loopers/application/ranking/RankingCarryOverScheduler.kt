package com.loopers.application.ranking

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

@Component
class RankingCarryOverScheduler(
    private val redisZSetTemplate: RedisZSetTemplate,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * ?")
    fun carryOver() {
        val yesterday = LocalDate.now().minusDays(1)
        val today = LocalDate.now()
        val yesterdayKey = RankingKeyGenerator.dailyKey(yesterday)
        val todayKey = RankingKeyGenerator.dailyKey(today)

        try {
            val count = redisZSetTemplate.unionStoreWithWeight(todayKey, yesterdayKey, rankingProperties.carryOverWeight)
            redisZSetTemplate.setTtlIfAbsent(todayKey, Duration.ofDays(rankingProperties.ttlDays))
            log.info("랭킹 Carry-Over 완료 [from={}, to={}, count={}]", yesterday, today, count)
        } catch (e: Exception) {
            log.error("랭킹 Carry-Over 실패 [from={}, to={}]", yesterday, today, e)
        }
    }
}
