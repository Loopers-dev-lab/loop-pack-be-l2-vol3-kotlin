package com.loopers.reconciliation.like

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class LikeCleanupReconciliationTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val deletedRows = jdbcTemplate.update(
            """
            DELETE pl FROM product_like pl
            JOIN product p ON pl.product_id = p.id
            WHERE p.status = 'DELETED'
            """,
        )

        if (deletedRows > 0) {
            log.warn("상품-좋아요 정리 보정: {}건", deletedRows)
        } else {
            log.info("상품-좋아요 정합성 이상 없음")
        }

        return RepeatStatus.FINISHED
    }
}
