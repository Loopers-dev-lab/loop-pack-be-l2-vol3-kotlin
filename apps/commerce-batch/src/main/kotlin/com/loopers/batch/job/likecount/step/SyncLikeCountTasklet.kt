package com.loopers.batch.job.likecount.step

import com.loopers.batch.job.likecount.LikeCountSyncJobConfig
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
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = LikeCountSyncJobConfig.JOB_NAME)
@Component
class SyncLikeCountTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        log.info("[SyncLikeCountTasklet] like_count 집계 시작")

        val resetCount = jdbcTemplate.update("UPDATE product SET like_count = 0")
        log.info("[SyncLikeCountTasklet] like_count 리셋 완료 ({}건)", resetCount)

        val syncCount = jdbcTemplate.update(
            """
            UPDATE product p
            JOIN (
                SELECT product_id, COUNT(*) AS cnt
                FROM product_like
                GROUP BY product_id
            ) pl ON p.id = pl.product_id
            SET p.like_count = pl.cnt
            """.trimIndent(),
        )
        log.info("[SyncLikeCountTasklet] like_count 동기화 완료 ({}건)", syncCount)

        return RepeatStatus.FINISHED
    }
}
