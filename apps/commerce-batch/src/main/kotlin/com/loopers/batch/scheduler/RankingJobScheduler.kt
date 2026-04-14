package com.loopers.batch.scheduler

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
@Profile("scheduler")
class RankingJobScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier(WeeklyRankingJobConfig.JOB_NAME) private val weeklyRankingJob: Job,
    @Qualifier(MonthlyRankingJobConfig.JOB_NAME) private val monthlyRankingJob: Job,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(RankingJobScheduler::class.java)
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    // 매주 월요일 01:30 — 지난 주 집계
    @Scheduled(cron = "0 30 1 * * MON", zone = "Asia/Seoul")
    fun runWeeklyRankingJob() {
        val baseDate = LocalDate.now(clock).minusWeeks(1).format(formatter)
        log.info("WeeklyRankingJob 스케줄 실행 - baseDate=$baseDate (지난 주)")
        jobLauncher.run(weeklyRankingJob, buildParams(baseDate))
    }

    // 매월 1일 01:30 — 지난 달 집계
    @Scheduled(cron = "0 30 1 1 * *", zone = "Asia/Seoul")
    fun runMonthlyRankingJob() {
        val baseDate = LocalDate.now(clock).minusMonths(1).format(formatter)
        log.info("MonthlyRankingJob 스케줄 실행 - baseDate=$baseDate (지난 달)")
        jobLauncher.run(monthlyRankingJob, buildParams(baseDate))
    }

    private fun buildParams(baseDate: String) =
        JobParametersBuilder()
            .addString("baseDate", baseDate)
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
}
