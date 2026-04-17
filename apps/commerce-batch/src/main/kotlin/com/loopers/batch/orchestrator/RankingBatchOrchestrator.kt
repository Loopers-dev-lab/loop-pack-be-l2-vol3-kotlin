package com.loopers.batch.orchestrator

import com.loopers.batch.job.monthly.MonthlyRankJobConfig
import com.loopers.batch.job.snapshot.DailySnapshotJobConfig
import com.loopers.batch.job.weekly.WeeklyRankJobConfig
import com.loopers.common.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingBatchOrchestrator(
    private val jobLauncher: JobLauncher,
    private val jobExplorer: JobExplorer,
    @param:Qualifier(DailySnapshotJobConfig.JOB_NAME) private val dailyJob: Job,
    @param:Qualifier(WeeklyRankJobConfig.JOB_NAME) private val weeklyJob: Job,
    @param:Qualifier(MonthlyRankJobConfig.JOB_NAME) private val monthlyJob: Job,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${batch.ranking.orchestrator.cron}", zone = "Asia/Seoul")
    fun runAll() {
        if (anyJobRunning()) {
            log.warn("이전 랭킹 배치가 아직 진행 중 — 이번 트리거 skip")
            return
        }
        val targetDate = DateUtils.yesterdayKst()
        launchSequentially(targetDate)
    }

    fun launchSequentially(targetDate: LocalDate) {
        if (!launch("Daily", dailyJob, targetDate)) return
        if (!launch("Weekly", weeklyJob, targetDate)) return
        launch("Monthly", monthlyJob, targetDate)
    }

    private fun launch(name: String, job: Job, targetDate: LocalDate): Boolean {
        val params = JobParametersBuilder()
            .addLocalDate(PARAM_TARGET_DATE, targetDate)
            .addLong(PARAM_TRIGGER, System.currentTimeMillis())
            .toJobParameters()
        val execution = jobLauncher.run(job, params)
        val success = execution.status == BatchStatus.COMPLETED
        if (success) {
            log.info("$name Job 완료 [targetDate={}]", targetDate)
        } else {
            log.error("$name Job 실패 [targetDate={}, status={}] — 이후 Job 중단", targetDate, execution.status)
        }
        return success
    }

    private fun anyJobRunning(): Boolean =
        JOB_NAMES.any { jobExplorer.findRunningJobExecutions(it).isNotEmpty() }

    companion object {
        const val PARAM_TARGET_DATE = "targetDate"
        const val PARAM_TRIGGER = "trigger"
        private val JOB_NAMES = listOf(
            DailySnapshotJobConfig.JOB_NAME,
            WeeklyRankJobConfig.JOB_NAME,
            MonthlyRankJobConfig.JOB_NAME,
        )
    }
}
