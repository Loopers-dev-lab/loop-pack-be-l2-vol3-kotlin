package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankResult
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.time.LocalDate

class RankingAggregationWriter(
    private val batchInsertAction: (List<ProductRankResult>, LocalDate, LocalDate) -> Unit,
    private val periodStartDate: LocalDate,
    private val periodEndDate: LocalDate,
) : ItemWriter<ProductRankResult> {

    override fun write(chunk: Chunk<out ProductRankResult>) {
        batchInsertAction(chunk.items.toList(), periodStartDate, periodEndDate)
    }
}
