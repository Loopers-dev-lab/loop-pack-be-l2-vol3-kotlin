package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.WeeklyRankingQueryDao
import com.loopers.batch.job.ranking.WeeklyWindow
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@StepScope
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
        contribution.incrementWriteCount(top100.size.toLong())
        return RepeatStatus.FINISHED
    }
}
