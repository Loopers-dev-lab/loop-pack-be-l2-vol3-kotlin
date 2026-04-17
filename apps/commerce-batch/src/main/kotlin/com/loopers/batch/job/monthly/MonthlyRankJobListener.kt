package com.loopers.batch.job.monthly

import com.loopers.common.DateUtils
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class MonthlyRankJobListener(
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) : JobExecutionListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun beforeJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE) ?: return
        val periodStart = targetDate.minusDays((MONTH_SPAN_DAYS - 1).toLong())
        val yearMonth = DateUtils.formatYearMonth(targetDate)
        val presentDays = (0 until MONTH_SPAN_DAYS).count { offset ->
            productMetricsDailyRepository.countDailyOn(periodStart.plusDays(offset.toLong())) > 0
        }
        when (presentDays) {
            0 -> log.warn(
                "MonthlyRank 집계 대상 daily 데이터가 없음 [yearMonth={}, periodStart={}, periodEnd={}]",
                yearMonth,
                periodStart,
                targetDate,
            )
            in 1 until MONTH_SPAN_DAYS -> log.warn(
                "MonthlyRank 대상 기간 중 누락일 있음 [yearMonth={}, periodStart={}, periodEnd={}, 존재일수={}/{}]",
                yearMonth,
                periodStart,
                targetDate,
                presentDays,
                MONTH_SPAN_DAYS,
            )
            else -> log.info(
                "MonthlyRank 대상 기간 확인 [yearMonth={}, periodStart={}, periodEnd={}, 존재일수={}]",
                yearMonth,
                periodStart,
                targetDate,
                presentDays,
            )
        }
    }

    override fun afterJob(jobExecution: JobExecution) {
        val targetDate = jobExecution.jobParameters.getLocalDate(PARAM_TARGET_DATE)
        val yearMonth = targetDate?.let { DateUtils.formatYearMonth(it) }
        val readCount = jobExecution.stepExecutions.sumOf { it.readCount }
        val writeCount = jobExecution.stepExecutions.sumOf { it.writeCount }
        val skipCount = jobExecution.stepExecutions.sumOf { it.skipCount }
        log.info(
            "MonthlyRank 종료 [yearMonth={}, status={}, read={}, write={}, skip={}]",
            yearMonth,
            jobExecution.status,
            readCount,
            writeCount,
            skipCount,
        )
    }

    companion object {
        const val PARAM_TARGET_DATE = "targetDate"
        const val MONTH_SPAN_DAYS = 30
    }
}
