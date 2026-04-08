package com.loopers.batch.job.queue.step

import com.loopers.batch.job.queue.QueueTokenJobConfig
import com.loopers.config.QueueProperties
import com.loopers.domain.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = QueueTokenJobConfig.JOB_NAME)
@Component
class QueueTokenTasklet(
    private val queueService: QueueService,
    private val queueProperties: QueueProperties,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val processed = queueService.popAndIssueTokens(queueProperties.batchSize)
        if (processed > 0) {
            log.info("[Queue] {}명에게 토큰 발급 완료", processed)
        }
        return RepeatStatus.FINISHED
    }
}
