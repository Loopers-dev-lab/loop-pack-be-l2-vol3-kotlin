package com.loopers.batch.orchestrator

import com.loopers.batch.job.monthly.MonthlyRankJobConfig
import com.loopers.batch.job.snapshot.DailySnapshotJobConfig
import com.loopers.batch.job.weekly.WeeklyRankJobConfig
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import java.time.LocalDate

class RankingBatchOrchestratorTest {

    private val jobLauncher = mockk<JobLauncher>()
    private val jobExplorer = mockk<JobExplorer> {
        every { findRunningJobExecutions(any()) } returns emptySet()
    }
    private val dailyJob = mockk<Job>()
    private val weeklyJob = mockk<Job>()
    private val monthlyJob = mockk<Job>()

    private val orchestrator = RankingBatchOrchestrator(jobLauncher, jobExplorer, dailyJob, weeklyJob, monthlyJob)

    private val targetDate = LocalDate.of(2026, 4, 16)

    @Test
    fun `모든 Job이 COMPLETED면 Daily Weekly Monthly를 순차로 실행한다`() {
        every { jobLauncher.run(dailyJob, any()) } returns execution(BatchStatus.COMPLETED)
        every { jobLauncher.run(weeklyJob, any()) } returns execution(BatchStatus.COMPLETED)
        every { jobLauncher.run(monthlyJob, any()) } returns execution(BatchStatus.COMPLETED)

        orchestrator.launchSequentially(targetDate)

        verify { jobLauncher.run(dailyJob, any<JobParameters>()) }
        verify { jobLauncher.run(weeklyJob, any<JobParameters>()) }
        verify { jobLauncher.run(monthlyJob, any<JobParameters>()) }
    }

    @Test
    fun `Daily가 실패하면 Weekly Monthly는 실행되지 않는다`() {
        every { jobLauncher.run(dailyJob, any()) } returns execution(BatchStatus.FAILED)

        orchestrator.launchSequentially(targetDate)

        verify(exactly = 1) { jobLauncher.run(dailyJob, any<JobParameters>()) }
        verify(exactly = 0) { jobLauncher.run(weeklyJob, any<JobParameters>()) }
        verify(exactly = 0) { jobLauncher.run(monthlyJob, any<JobParameters>()) }
        confirmVerified(jobLauncher)
    }

    @Test
    fun `Weekly가 실패하면 Monthly는 실행되지 않는다`() {
        every { jobLauncher.run(dailyJob, any()) } returns execution(BatchStatus.COMPLETED)
        every { jobLauncher.run(weeklyJob, any()) } returns execution(BatchStatus.FAILED)

        orchestrator.launchSequentially(targetDate)

        verify(exactly = 1) { jobLauncher.run(dailyJob, any<JobParameters>()) }
        verify(exactly = 1) { jobLauncher.run(weeklyJob, any<JobParameters>()) }
        verify(exactly = 0) { jobLauncher.run(monthlyJob, any<JobParameters>()) }
    }

    @Test
    fun `이전 실행이 아직 진행 중이면 runAll은 Job을 트리거하지 않는다`() {
        val runningExplorer = mockk<JobExplorer> {
            every { findRunningJobExecutions(DailySnapshotJobConfig.JOB_NAME) } returns setOf(mockk())
            every { findRunningJobExecutions(WeeklyRankJobConfig.JOB_NAME) } returns emptySet()
            every { findRunningJobExecutions(MonthlyRankJobConfig.JOB_NAME) } returns emptySet()
        }
        val sut = RankingBatchOrchestrator(jobLauncher, runningExplorer, dailyJob, weeklyJob, monthlyJob)

        sut.runAll()

        verify(exactly = 0) { jobLauncher.run(any<Job>(), any<JobParameters>()) }
    }

    private fun execution(status: BatchStatus): JobExecution = mockk {
        every { this@mockk.status } returns status
    }
}
