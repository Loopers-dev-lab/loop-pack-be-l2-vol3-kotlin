package com.loopers.batch.job.ranking

import com.loopers.infrastructure.ranking.MonthlyProductRankingEntity
import com.loopers.infrastructure.ranking.MonthlyProductRankingJpaRepository
import com.loopers.infrastructure.ranking.RankingPeriodDateRangeResolver
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
class MonthlyProductRankingItemWriter(
    private val monthlyProductRankingJpaRepository: MonthlyProductRankingJpaRepository,
    @Value("#{jobParameters['targetDate']}") private val targetDate: String?,
) : ItemWriter<AggregatedProductRankingRow>, StepExecutionListener {
    private lateinit var monthStartDate: LocalDate
    private lateinit var monthEndDate: LocalDate
    private var rankingSequence: Long = 0L

    override fun beforeStep(stepExecution: StepExecution) {
        val dateRange = RankingPeriodDateRangeResolver.monthly(
            requireNotNull(targetDate) { "targetDate job parameter is required" },
        )
        monthStartDate = dateRange.startDate
        monthEndDate = dateRange.endDate
        rankingSequence = 0L
        monthlyProductRankingJpaRepository.deleteAllByMonthStartDate(monthStartDate)
    }

    override fun write(chunk: Chunk<out AggregatedProductRankingRow>) {
        if (chunk.isEmpty) {
            return
        }

        monthlyProductRankingJpaRepository.saveAll(
            chunk.items.map { item ->
                rankingSequence += 1
                MonthlyProductRankingEntity(
                    monthStartDate = monthStartDate,
                    monthEndDate = monthEndDate,
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
