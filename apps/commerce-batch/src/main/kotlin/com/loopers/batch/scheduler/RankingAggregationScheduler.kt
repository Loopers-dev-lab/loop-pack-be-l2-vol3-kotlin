package com.loopers.batch.scheduler

import com.loopers.batch.job.ranking.aggregate.MonthlyRankingAggregationJobConfig
import com.loopers.batch.job.ranking.aggregate.PeriodKeyResolver
import com.loopers.batch.job.ranking.aggregate.RankingPeriod
import com.loopers.batch.job.ranking.aggregate.WeeklyRankingAggregationJobConfig
import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["ranking.scheduler.enabled"], havingValue = "true")
class RankingAggregationScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier(WeeklyRankingAggregationJobConfig.JOB_NAME) private val weeklyJob: Job,
    @Qualifier(MonthlyRankingAggregationJobConfig.JOB_NAME) private val monthlyJob: Job,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 10 0 ? * MON", zone = "Asia/Seoul")
    fun runWeekly() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        val periodKey = PeriodKeyResolver.resolveWeekKey(yesterday)
        val (startDate, endDate) = PeriodKeyResolver.weekRange(yesterday)

        val params = JobParametersBuilder()
            .addString("targetDate", yesterday.toString())
            .addString("period", RankingPeriod.WEEKLY.name)
            .addString("periodKey", periodKey)
            .addString("startDate", startDate.toString())
            .addString("endDate", endDate.toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        log.info("[RankingAggregationScheduler] 주간 랭킹 집계 시작: periodKey={}, range={}/{}", periodKey, startDate, endDate)
        jobLauncher.run(weeklyJob, params)
    }

    @Scheduled(cron = "0 20 0 1 * ?", zone = "Asia/Seoul")
    fun runMonthly() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        val periodKey = PeriodKeyResolver.resolveMonthKey(yesterday)
        val (startDate, endDate) = PeriodKeyResolver.monthRange(yesterday)

        val params = JobParametersBuilder()
            .addString("targetDate", yesterday.toString())
            .addString("period", RankingPeriod.MONTHLY.name)
            .addString("periodKey", periodKey)
            .addString("startDate", startDate.toString())
            .addString("endDate", endDate.toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        log.info("[RankingAggregationScheduler] 월간 랭킹 집계 시작: periodKey={}, range={}/{}", periodKey, startDate, endDate)
        jobLauncher.run(monthlyJob, params)
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
