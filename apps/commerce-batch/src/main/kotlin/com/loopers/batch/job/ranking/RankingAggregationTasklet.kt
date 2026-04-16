package com.loopers.batch.job.ranking

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@StepScope
@Component
class RankingAggregationTasklet(
    private val redisTemplate: RedisTemplate<String, String>,
) : Tasklet {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private const val TEMP_KEY_PREFIX = "ranking:batch:tmp:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }

    @Value("#{jobParameters['targetDate']}")
    private lateinit var targetDate: String

    @Value("#{jobParameters['aggregationType']}")
    private lateinit var aggregationType: String // WEEKLY or MONTHLY

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val date = LocalDate.parse(targetDate, DATE_FORMATTER)

        val sourceKeys = when (aggregationType) {
            "WEEKLY" -> buildWeeklyKeys(date)
            "MONTHLY" -> buildMonthlyKeys(date)
            else -> throw IllegalArgumentException("Unknown aggregationType: $aggregationType")
        }

        val weights = DoubleArray(sourceKeys.size) { 1.0 }
        val tempKey = "$TEMP_KEY_PREFIX${UUID.randomUUID()}"

        val count = zunionstore(tempKey, sourceKeys, weights)

        if (count == 0L) {
            redisTemplate.delete(tempKey)
            return RepeatStatus.FINISHED
        }

        // Set TTL to prevent leak in case of app crash
        redisTemplate.expire(tempKey, Duration.ofSeconds(300))

        // Store tempKey in ExecutionContext for ChunkReader to use
        chunkContext.stepContext.stepExecution.jobExecution.executionContext.put("tempKey", tempKey)
        chunkContext.stepContext.stepExecution.jobExecution.executionContext.put("aggregationType", aggregationType)
        chunkContext.stepContext.stepExecution.jobExecution.executionContext.put("targetDate", targetDate)

        return RepeatStatus.FINISHED
    }

    private fun buildWeeklyKeys(date: LocalDate): List<String> {
        val mondayOfWeek = date.minusDays(date.dayOfWeek.value - 1L)
        return (0..6).map { dayOffset ->
            key(mondayOfWeek.plusDays(dayOffset.toLong()))
        }
    }

    private fun buildMonthlyKeys(date: LocalDate): List<String> {
        val firstDay = date.withDayOfMonth(1)
        val lastDay = date.withDayOfMonth(date.lengthOfMonth())
        return generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { it <= lastDay }
            .map { key(it) }
            .toList()
    }

    private fun zunionstore(destKey: String, sourceKeys: List<String>, weights: DoubleArray): Long {
        val result = redisTemplate.execute(
            object : RedisCallback<Long> {
                override fun doInRedis(connection: RedisConnection): Long {
                    return connection.zSetCommands().zUnionStore(
                        destKey.toByteArray(Charsets.UTF_8),
                        Aggregate.SUM,
                        Weights.of(*weights),
                        *sourceKeys.map { it.toByteArray(Charsets.UTF_8) }.toTypedArray(),
                    ) ?: 0L
                }
            },
        )
        return result ?: 0L
    }

    private fun key(date: LocalDate): String = "$KEY_PREFIX${date.format(DATE_FORMATTER)}"
}
