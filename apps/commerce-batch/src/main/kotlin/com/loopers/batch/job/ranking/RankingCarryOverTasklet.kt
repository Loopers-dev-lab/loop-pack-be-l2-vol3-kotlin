package com.loopers.batch.job.ranking

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = RankingCarryOverJobConfig.JOB_NAME)
@Component
class RankingCarryOverTasklet(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        // 자정 이후 실행 기준: yesterday → today로 carry-over
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val yesterdayKey = "${KEY_PREFIX}:${yesterday.format(FORMATTER)}"
        val todayKey = "${KEY_PREFIX}:${today.format(FORMATTER)}"

        val yesterdaySize = redisTemplate.opsForZSet().size(yesterdayKey) ?: 0L
        if (yesterdaySize == 0L) {
            log.info("[RankingCarryOver] 전일 랭킹 데이터 없음, carry-over 스킵: key={}", yesterdayKey)
            return RepeatStatus.FINISHED
        }

        // ZUNIONSTORE: today = (today * 1.0) + (yesterday * 0.1)
        // 이미 오늘 키에 데이터가 있으면 유지(weight 1.0) + 전일 데이터의 10% 합산
        redisTemplate.opsForZSet().unionAndStore(
            todayKey,
            listOf(yesterdayKey),
            todayKey,
            org.springframework.data.redis.connection.zset.Aggregate.SUM,
            org.springframework.data.redis.connection.zset.Weights.of(1.0, CARRY_OVER_WEIGHT),
        )
        redisTemplate.expire(todayKey, TTL)

        val todaySize = redisTemplate.opsForZSet().size(todayKey) ?: 0L
        log.info(
            "[RankingCarryOver] carry-over 완료: from={} ({}건) → to={} ({}건, weight={})",
            yesterdayKey,
            yesterdaySize,
            todayKey,
            todaySize,
            CARRY_OVER_WEIGHT,
        )

        return RepeatStatus.FINISHED
    }

    companion object {
        private const val KEY_PREFIX = "ranking:all"
        private const val CARRY_OVER_WEIGHT = 0.1
        private val TTL = Duration.ofDays(2)
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
