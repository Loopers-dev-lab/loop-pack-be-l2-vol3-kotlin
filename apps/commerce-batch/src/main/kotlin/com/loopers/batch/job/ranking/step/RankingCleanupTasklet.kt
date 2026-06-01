package com.loopers.batch.job.ranking.step

import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

class RankingCleanupTasklet(
    private val periodLabel: String,
    private val periodStartDate: LocalDate,
    private val deleteAction: (LocalDate) -> Unit,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        log.info("[RankingCleanup] period={}, periodStartDate={}", periodLabel, periodStartDate)
        deleteAction(periodStartDate)
        return RepeatStatus.FINISHED
    }
}
