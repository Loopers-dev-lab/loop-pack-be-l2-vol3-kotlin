package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.RankingCarryOverJobConfig
import com.loopers.domain.ranking.RankingCarryOverService
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = RankingCarryOverJobConfig.JOB_NAME)
@Component
class RankingCarryOverTasklet(
    private val rankingCarryOverService: RankingCarryOverService,
    @param:Value("#{jobParameters['requestDate']}") private val requestDate: String,
) : Tasklet {
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val baseDate = LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE)
        rankingCarryOverService.execute(baseDate)
        return RepeatStatus.FINISHED
    }
}
