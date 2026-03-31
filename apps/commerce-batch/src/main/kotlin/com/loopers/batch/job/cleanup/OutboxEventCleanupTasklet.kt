package com.loopers.batch.job.cleanup

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = OutboxEventCleanupJobConfig.JOB_NAME)
@Component
class OutboxEventCleanupTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        log.info("[OutboxEventCleanup] 7일 이전 발행 완료 이벤트 정리 시작")

        val deletedCount = jdbcTemplate.update(
            "DELETE FROM outbox_event WHERE published_at IS NOT NULL AND published_at < DATE_SUB(NOW(), INTERVAL 7 DAY)",
        )

        log.info("[OutboxEventCleanup] 정리 완료 ({}건 삭제)", deletedCount)
        return RepeatStatus.FINISHED
    }
}
