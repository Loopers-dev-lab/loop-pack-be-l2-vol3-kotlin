package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.ProductMetricsRow
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.time.LocalDate

class RankingWriter<E>(
    private val rankingDate: LocalDate,
    private val entityFactory: (ProductMetricsRow, Int, LocalDate) -> E,
    private val saveAction: (List<E>) -> Unit,
) : ItemWriter<ProductMetricsRow>, StepExecutionListener {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var stepExecution: StepExecution
    private var currentRank = 0

    override fun beforeStep(stepExecution: StepExecution) {
        this.stepExecution = stepExecution
        currentRank = stepExecution.executionContext.getInt(KEY_CURRENT_RANK, 0)
    }

    override fun write(chunk: Chunk<out ProductMetricsRow>) {
        val entities = chunk.items.map { row ->
            currentRank++
            entityFactory(row, currentRank, rankingDate)
        }
        saveAction(entities)
        stepExecution.executionContext.putInt(KEY_CURRENT_RANK, currentRank)
        log.info("랭킹 적재 완료: ${chunk.items.size}건 (누적 ${currentRank}위까지)")
    }

    companion object {
        private const val KEY_CURRENT_RANK = "currentRank"
    }
}
