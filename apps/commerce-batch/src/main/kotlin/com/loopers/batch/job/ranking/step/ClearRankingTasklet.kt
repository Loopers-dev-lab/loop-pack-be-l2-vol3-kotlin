package com.loopers.batch.job.ranking.step

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

class ClearRankingTasklet(
    private val deleteAction: (String) -> Unit,
    private val periodDate: String,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        deleteAction(periodDate)
        log.info("[ClearRanking] Cleared existing ranking for period={}", periodDate)
        return RepeatStatus.FINISHED
    }
}
