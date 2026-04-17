package com.loopers.batch.job.snapshot

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class DailySnapshotJobListener(
    private val redisZSetTemplate: RedisZSetTemplate,
    private val properties: DailySnapshotProperties,
) : JobExecutionListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun beforeJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE) ?: return
        val key = RankingKeyGenerator.dailyKey(targetDate)

        if (!redisZSetTemplate.hasKey(key)) {
            log.warn("DailySnapshot 대상 키 없음 — TTL 만료로 데이터 없음, 백필 필요 가능 [date={}, key={}]", targetDate, key)
            return
        }

        val ttlSeconds = redisZSetTemplate.getExpireSeconds(key)
        when {
            ttlSeconds < 0 -> log.info("DailySnapshot 대상 키 TTL 없음 [date={}]", targetDate)
            ttlSeconds < properties.ttlWarnThresholdHours * SECONDS_PER_HOUR ->
                log.warn(
                    "DailySnapshot 대상 키 TTL 임계치 미만 — 곧 만료됨 [date={}, 잔여={}h]",
                    targetDate,
                    ttlSeconds / SECONDS_PER_HOUR,
                )
            else -> log.info("DailySnapshot 대상 키 TTL 정상 [date={}, 잔여={}h]", targetDate, ttlSeconds / SECONDS_PER_HOUR)
        }
    }

    override fun afterJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE)
        val readCount = jobExecution.stepExecutions.sumOf { it.readCount }
        val writeCount = jobExecution.stepExecutions.sumOf { it.writeCount }
        val skipCount = jobExecution.stepExecutions.sumOf { it.skipCount }
        log.info(
            "DailySnapshot 종료 [date={}, status={}, read={}, write={}, skip={}]",
            targetDate,
            jobExecution.status,
            readCount,
            writeCount,
            skipCount,
        )
    }

    companion object {
        const val PARAM_TARGET_DATE = "targetDate"
        private const val SECONDS_PER_HOUR = 3_600L
    }
}
