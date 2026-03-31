package com.loopers.reconciliation.product

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProductCascadeReconciliationTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val updatedRows = jdbcTemplate.update(
            """
            UPDATE product p
            JOIN brand b ON p.brand_id = b.id
            SET p.status = 'DELETED', p.deleted_at = NOW(), p.updated_at = NOW()
            WHERE b.status = 'DELETED'
              AND p.status = 'ACTIVE'
            """,
        )

        if (updatedRows > 0) {
            log.warn("브랜드-상품 캐스케이드 보정: {}건", updatedRows)
        } else {
            log.info("브랜드-상품 캐스케이드 정합성 이상 없음")
        }

        return RepeatStatus.FINISHED
    }
}
