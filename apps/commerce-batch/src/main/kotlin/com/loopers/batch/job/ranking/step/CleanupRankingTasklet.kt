package com.loopers.batch.job.ranking.step

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

class CleanupRankingTasklet(
    private val rankingDate: LocalDate,
    private val deleteAction: (LocalDate) -> Unit,
    private val tableName: String,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        log.info("$tableName 기존 데이터 삭제: rankingDate=$rankingDate")
        deleteAction(rankingDate)
        return RepeatStatus.FINISHED
    }
}
