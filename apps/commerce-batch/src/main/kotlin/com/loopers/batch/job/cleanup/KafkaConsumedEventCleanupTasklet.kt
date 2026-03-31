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
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = KafkaConsumedEventCleanupJobConfig.JOB_NAME)
@Component
class KafkaConsumedEventCleanupTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        log.info("[KafkaConsumedEventCleanup] 30일 이전 소비 이벤트 정리 시작")

        val deletedCount = jdbcTemplate.update(
            "DELETE FROM kafka_consumed_event WHERE handled_at < DATE_SUB(NOW(), INTERVAL 30 DAY)",
        )

        log.info("[KafkaConsumedEventCleanup] 정리 완료 ({}건 삭제)", deletedCount)
        return RepeatStatus.FINISHED
    }
}
