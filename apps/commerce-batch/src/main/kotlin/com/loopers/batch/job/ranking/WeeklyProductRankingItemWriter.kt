package com.loopers.batch.job.ranking

import com.loopers.infrastructure.ranking.RankingPeriodDateRangeResolver
import com.loopers.infrastructure.ranking.WeeklyProductRankingEntity
import com.loopers.infrastructure.ranking.WeeklyProductRankingJpaRepository
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@Component
class WeeklyProductRankingItemWriter(
    private val weeklyProductRankingJpaRepository: WeeklyProductRankingJpaRepository,
    @Value("#{jobParameters['targetDate']}") private val targetDate: String?,
) : ItemWriter<AggregatedProductRankingRow>, StepExecutionListener {
    private lateinit var weekStartDate: LocalDate
    private lateinit var weekEndDate: LocalDate
    private var rankingSequence: Long = 0L

    override fun beforeStep(stepExecution: StepExecution) {
        val dateRange = RankingPeriodDateRangeResolver.weekly(
            requireNotNull(targetDate) { "targetDate job parameter is required" },
        )
        weekStartDate = dateRange.startDate
        weekEndDate = dateRange.endDate
        rankingSequence = 0L
        weeklyProductRankingJpaRepository.deleteAllByWeekStartDate(weekStartDate)
    }

    override fun write(chunk: Chunk<out AggregatedProductRankingRow>) {
        if (chunk.isEmpty) {
            return
        }

        weeklyProductRankingJpaRepository.saveAll(
            chunk.items.map { item ->
                rankingSequence += 1
                WeeklyProductRankingEntity(
                    weekStartDate = weekStartDate,
                    weekEndDate = weekEndDate,
                    productId = item.productId,
                    ranking = rankingSequence,
                    score = item.score,
                    likeCount = item.likeCount,
                    viewCount = item.viewCount,
                    salesCount = item.salesCount,
                )
            },
        )
    }

    override fun afterStep(stepExecution: StepExecution): ExitStatus? = null
}
