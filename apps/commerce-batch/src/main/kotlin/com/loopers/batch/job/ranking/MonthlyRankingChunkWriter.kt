package com.loopers.batch.job.ranking

import com.loopers.batch.infrastructure.ranking.MvProductRankMonthlyJpaRepository
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@StepScope
@Component
class MonthlyRankingChunkWriter(
    private val monthlyRepository: MvProductRankMonthlyJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
) : ItemWriter<RankingScoreContribution>, StepExecutionListener {

    @Value("#{jobParameters['targetDate']}")
    private lateinit var targetDate: String

    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private var isFirstChunk = true

    override fun beforeStep(stepExecution: StepExecution) {}

    @Transactional
    override fun write(chunk: Chunk<out RankingScoreContribution>) {
        val date = LocalDate.parse(targetDate, dateFormatter)
        val yearMonth = getYearMonth(date)

        if (isFirstChunk) {
            monthlyRepository.deleteByYearMonth(yearMonth)
            isFirstChunk = false
        }

        if (chunk.items.isEmpty()) return

        jdbcTemplate.batchUpdate(
            UPSERT_SQL,
            chunk.items.map { arrayOf(it.productId, yearMonth, it.score) },
        )
    }

    override fun afterStep(stepExecution: StepExecution): ExitStatus = ExitStatus.COMPLETED

    private fun getYearMonth(date: LocalDate): String {
        return String.format("%d-%02d", date.year, date.monthValue)
    }

    companion object {
        private const val UPSERT_SQL = """
            INSERT INTO mv_product_rank_monthly (product_id, year_month, score, rank, updated_at)
            VALUES (?, ?, ?, 0, NOW())
            ON DUPLICATE KEY UPDATE score = score + VALUES(score), updated_at = NOW()
        """
    }
}
