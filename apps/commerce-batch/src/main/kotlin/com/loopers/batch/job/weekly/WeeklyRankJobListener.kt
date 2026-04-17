package com.loopers.batch.job.weekly

import com.loopers.domain.ranking.ProductMetricsDailyRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class WeeklyRankJobListener(
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) : JobExecutionListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun beforeJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE) ?: return
        val weekStart = targetDate.minusDays((WEEK_SPAN_DAYS - 1).toLong())
        val presentDays = (0 until WEEK_SPAN_DAYS).count { offset ->
            productMetricsDailyRepository.countDailyOn(weekStart.plusDays(offset.toLong())) > 0
        }
        when (presentDays) {
            0 -> log.warn(
                "WeeklyRank 집계 대상 daily 데이터가 없음 [weekStart={}, weekEnd={}]",
                weekStart,
                targetDate,
            )
            in 1 until WEEK_SPAN_DAYS -> log.warn(
                "WeeklyRank 대상 기간 중 누락일 있음 [weekStart={}, weekEnd={}, 존재일수={}/{}]",
                weekStart,
                targetDate,
                presentDays,
                WEEK_SPAN_DAYS,
            )
            else -> log.info(
                "WeeklyRank 대상 기간 확인 [weekStart={}, weekEnd={}, 존재일수={}]",
                weekStart,
                targetDate,
                presentDays,
            )
        }
    }

    override fun afterJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE)
        val readCount = jobExecution.stepExecutions.sumOf { it.readCount }
        val writeCount = jobExecution.stepExecutions.sumOf { it.writeCount }
        val skipCount = jobExecution.stepExecutions.sumOf { it.skipCount }
        log.info(
            "WeeklyRank 종료 [weekEnd={}, status={}, read={}, write={}, skip={}]",
            targetDate,
            jobExecution.status,
            readCount,
            writeCount,
            skipCount,
        )
    }

    companion object {
        const val PARAM_TARGET_DATE = "targetDate"
        const val WEEK_SPAN_DAYS = 7
    }
}
