package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class RankingColdStartScheduler(
    private val rankingScoreUpdater: RankingScoreUpdater,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 23:50에 전일 점수의 10%를 다음 날 키로 carry-over한다.
     * ZUNIONSTORE 대신 수동 복사로 처리한다.
     * (Lettuce의 ZUNIONSTORE는 WEIGHTS 파라미터 지정이 불편하므로 명시적 구현)
     */
    @Scheduled(cron = "0 50 23 * * *")
    fun carryOverScores() {
        carryOverScores(LocalDate.now())
    }

    // 날짜를 주입받는 오버로드: 테스트가 자정 경계에 걸려도 키가 어긋나지 않는다.
    fun carryOverScores(today: LocalDate) {
        val tomorrow = today.plusDays(1)
        val sourceKey = rankingScoreUpdater.buildKey(today)
        val targetKey = rankingScoreUpdater.buildKey(tomorrow)

        val topEntries = masterRedisTemplate.opsForZSet()
            .reverseRangeWithScores(sourceKey, 0, CARRY_OVER_LIMIT - 1)

        if (topEntries.isNullOrEmpty()) {
            log.info("carry-over 대상 없음. sourceKey={}", sourceKey)
            return
        }

        for (entry in topEntries) {
            val member = entry.value ?: continue
            val carryScore = (entry.score ?: 0.0) * CARRY_OVER_WEIGHT
            if (carryScore > 0) {
                masterRedisTemplate.opsForZSet().incrementScore(targetKey, member, carryScore)
            }
        }
        masterRedisTemplate.expire(targetKey, TTL_DAYS, TimeUnit.DAYS)

        log.info(
            "콜드 스타트 carry-over 완료. source={}, target={}, count={}",
            sourceKey,
            targetKey,
            topEntries.size,
        )
    }

    companion object {
        private const val CARRY_OVER_WEIGHT = 0.1
        private const val CARRY_OVER_LIMIT = 500L
        private const val TTL_DAYS = 2L
    }
}
