package com.loopers.batch.listener

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

@Component
class StepMonitorListener(
    private val meterRegistry: MeterRegistry,
) : StepExecutionListener {
    private val log = LoggerFactory.getLogger(StepMonitorListener::class.java)

    override fun beforeStep(stepExecution: StepExecution) {
        log.info("Step '${stepExecution.stepName}' 시작")
    }

    override fun afterStep(stepExecution: StepExecution): ExitStatus {
        val stepName = stepExecution.stepName
        val isFailed = stepExecution.failureExceptions.isNotEmpty()

        val duration = Duration.between(
            stepExecution.startTime,
            stepExecution.endTime ?: LocalDateTime.now(),
        )
        meterRegistry.timer("batch.step.duration", "step", stepName).record(duration)
        meterRegistry.counter("batch.step.write.count", "step", stepName)
            .increment(stepExecution.writeCount.toDouble())
        meterRegistry.counter("batch.step.result", "step", stepName, "status", if (isFailed) "failure" else "success")
            .increment()

        if (isFailed) {
            log.error(
                """
                    [에러 발생]
                    jobName: ${stepExecution.jobExecution.jobInstance.jobName}
                    exceptions:
                    ${stepExecution.failureExceptions.mapNotNull { it.message }.joinToString("\n")}
                """.trimIndent(),
            )
            // error 발생 시 slack 등 다른 채널로 모니터 전송
            return ExitStatus.FAILED
        }
        return ExitStatus.COMPLETED
    }
}
