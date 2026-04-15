package com.loopers.batch.interfaces

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class TriggerRequest(val baseDate: String)

data class TriggerResponse(
    val jobName: String,
    val baseDate: String,
    val status: String,
    val message: String? = null,
)

@RestController
@Profile("scheduler")
@RequestMapping("/internal/batch/ranking")
class BatchRankingController(
    private val jobLauncher: JobLauncher,
    @Qualifier(WeeklyRankingJobConfig.JOB_NAME) private val weeklyRankingJob: Job,
    @Qualifier(MonthlyRankingJobConfig.JOB_NAME) private val monthlyRankingJob: Job,
) {
    @PostMapping("/weekly")
    fun triggerWeekly(
        @RequestBody request: TriggerRequest,
    ): ResponseEntity<TriggerResponse> = trigger(WeeklyRankingJobConfig.JOB_NAME, weeklyRankingJob, request.baseDate)

    @PostMapping("/monthly")
    fun triggerMonthly(
        @RequestBody request: TriggerRequest,
    ): ResponseEntity<TriggerResponse> = trigger(MonthlyRankingJobConfig.JOB_NAME, monthlyRankingJob, request.baseDate)

    private fun trigger(
        jobName: String,
        job: Job,
        baseDate: String,
    ): ResponseEntity<TriggerResponse> {
        return try {
            val execution = jobLauncher.run(job, buildParams(baseDate))
            val message = execution.exitStatus.exitDescription.takeIf { it.isNotBlank() }?.take(500)
            when (execution.status) {
                BatchStatus.COMPLETED ->
                    ResponseEntity.ok(TriggerResponse(jobName, baseDate, execution.status.name, message))
                BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.UNKNOWN, BatchStatus.ABANDONED ->
                    ResponseEntity.internalServerError().body(TriggerResponse(jobName, baseDate, execution.status.name, message))
                else ->
                    ResponseEntity.accepted().body(TriggerResponse(jobName, baseDate, execution.status.name, message))
            }
        } catch (e: JobParametersInvalidException) {
            ResponseEntity.badRequest().body(
                TriggerResponse(jobName, baseDate, status = "INVALID", message = e.message),
            )
        }
    }

    private fun buildParams(baseDate: String) =
        JobParametersBuilder()
            .addString("baseDate", baseDate)
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
}
