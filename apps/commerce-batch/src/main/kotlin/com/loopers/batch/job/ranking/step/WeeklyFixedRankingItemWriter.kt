package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.FixedRankingAggregateRow
import com.loopers.domain.ranking.ProductRankWeeklyMvModel
import com.loopers.infrastructure.ranking.ProductRankWeeklyMvJpaRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@Component
class WeeklyFixedRankingItemWriter(
    @param:Value("#{jobExecutionContext['weekPeriodStartDate']}") private val periodStartDate: LocalDate,
    @param:Value("#{jobExecutionContext['weekPeriodEndDate']}") private val periodEndDate: LocalDate,
    private val productRankWeeklyMvJpaRepository: ProductRankWeeklyMvJpaRepository,
) : ItemWriter<FixedRankingAggregateRow> {
    private var nextRank: Long = 1L

    override fun write(chunk: Chunk<out FixedRankingAggregateRow>) {
        if (chunk.isEmpty) return

        productRankWeeklyMvJpaRepository.saveAll(
            chunk.items.map { item ->
                ProductRankWeeklyMvModel(
                    periodStartDate = periodStartDate,
                    periodEndDate = periodEndDate,
                    rankPosition = nextRank++,
                    productId = item.productId,
                    score = item.totalScore,
                )
            },
        )
    }
}
