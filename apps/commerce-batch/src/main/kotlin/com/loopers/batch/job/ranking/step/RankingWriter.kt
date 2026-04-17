package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.ProductMetricsRow
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.time.LocalDate

class RankingWriter<E>(
    private val rankingDate: LocalDate,
    private val entityFactory: (ProductMetricsRow, Int, LocalDate) -> E,
    private val saveAction: (List<E>) -> Unit,
) : ItemWriter<ProductMetricsRow> {
    private val log = LoggerFactory.getLogger(javaClass)
    private var currentRank = 0

    override fun write(chunk: Chunk<out ProductMetricsRow>) {
        val entities = chunk.items.map { row ->
            currentRank++
            entityFactory(row, currentRank, rankingDate)
        }
        saveAction(entities)
        log.info("랭킹 적재 완료: ${chunk.items.size}건 (누적 ${currentRank}위까지)")
    }
}
