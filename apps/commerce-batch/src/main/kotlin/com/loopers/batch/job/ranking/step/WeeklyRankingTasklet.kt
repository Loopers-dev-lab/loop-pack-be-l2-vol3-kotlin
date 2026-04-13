package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import com.loopers.batch.job.ranking.WeeklyRankingQueryDao
import com.loopers.batch.job.ranking.WeeklyWindow
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Component
class WeeklyRankingTasklet(
    private val queryDao: WeeklyRankingQueryDao,
    @param:Value("#{jobParameters['baseDate']}") private val baseDate: String,
) : Tasklet {
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val (periodKey, startDate, endDate) = WeeklyWindow.from(baseDate)
        val top100 = queryDao.selectTop100Aggregate(startDate, endDate)
        queryDao.deleteByPeriodKey(periodKey)
        queryDao.bulkInsert(periodKey, top100, startDate, endDate)
        return RepeatStatus.FINISHED
    }
}
