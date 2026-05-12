package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.FixedRankingAggregateRow
import com.loopers.domain.ranking.ProductRankMonthlyMvModel
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@Component
class MonthlyFixedRankingItemWriter(
    @param:Value("#{jobExecutionContext['monthPeriodStartDate']}") private val periodStartDate: LocalDate,
    @param:Value("#{jobExecutionContext['monthPeriodEndDate']}") private val periodEndDate: LocalDate,
    private val productRankMonthlyMvJpaRepository: ProductRankMonthlyMvJpaRepository,
) : ItemWriter<FixedRankingAggregateRow> {
    private var nextRank: Long = 1L

    override fun write(chunk: Chunk<out FixedRankingAggregateRow>) {
        if (chunk.isEmpty) return

        productRankMonthlyMvJpaRepository.saveAll(
            chunk.items.map { item ->
                ProductRankMonthlyMvModel(
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
