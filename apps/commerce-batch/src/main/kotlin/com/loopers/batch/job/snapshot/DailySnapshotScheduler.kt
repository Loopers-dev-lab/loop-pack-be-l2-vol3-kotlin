package com.loopers.batch.job.snapshot

import com.loopers.common.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailySnapshotScheduler(
    private val jobLauncher: JobLauncher,
    @param:Qualifier(DailySnapshotJobConfig.JOB_NAME) private val dailySnapshotJob: Job,
    private val jobExplorer: JobExplorer,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${batch.snapshot.daily.cron}", zone = "Asia/Seoul")
    fun run() {
        val running = jobExplorer.findRunningJobExecutions(DailySnapshotJobConfig.JOB_NAME)
        if (running.isNotEmpty()) {
            log.warn("DailySnapshot 이미 실행 중 — 트리거 skip [runningCount={}]", running.size)
            return
        }
        launch(DateUtils.yesterdayKst())
    }

    fun launch(targetDate: java.time.LocalDate) {
        val params = JobParametersBuilder()
            .addLocalDate(DailySnapshotJobListener.PARAM_TARGET_DATE, targetDate)
            .addLong("trigger", System.currentTimeMillis())
            .toJobParameters()
        jobLauncher.run(dailySnapshotJob, params)
    }
}
