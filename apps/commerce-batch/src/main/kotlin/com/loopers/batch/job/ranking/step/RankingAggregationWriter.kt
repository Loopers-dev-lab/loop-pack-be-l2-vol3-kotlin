package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankMonthlyRepository
import com.loopers.domain.ranking.ProductRankResult
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import com.loopers.domain.ranking.RankingPeriodType
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.time.LocalDate

class RankingAggregationWriter(
    private val periodType: RankingPeriodType,
    private val requestDate: LocalDate,
    private val weeklyRepository: ProductRankWeeklyRepository,
    private val monthlyRepository: ProductRankMonthlyRepository,
) : ItemWriter<ProductRankResult> {

    override fun write(chunk: Chunk<out ProductRankResult>) {
        val periodStartDate = periodType.periodStartDate(requestDate)
        val periodEndDate = periodType.periodEndDate(requestDate)
        val items = chunk.items.toList()

        when (periodType) {
            RankingPeriodType.WEEKLY -> weeklyRepository.batchInsert(items, periodStartDate, periodEndDate)
            RankingPeriodType.MONTHLY -> monthlyRepository.batchInsert(items, periodStartDate, periodEndDate)
        }
    }
}
