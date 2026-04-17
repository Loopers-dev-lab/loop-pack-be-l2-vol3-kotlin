package com.loopers.batch.job.ranking

import com.loopers.infrastructure.ranking.MonthlyProductRankingJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.Chunk
import java.time.LocalDate

class MonthlyProductRankingItemWriterTest {
    private val repository = mockk<MonthlyProductRankingJpaRepository>(relaxed = true)
    private val writer = MonthlyProductRankingItemWriter(repository, "20260416")

    @Test
    fun `월간_writer는_기존_월간_MV를_삭제하고_연속된_rank를_저장한다`() {
        val saved = slot<List<com.loopers.infrastructure.ranking.MonthlyProductRankingEntity>>()
        every { repository.saveAll(capture(saved)) } answers { saved.captured }

        writer.beforeStep(stepExecution())
        writer.write(
            Chunk(
                listOf(
                    AggregatedProductRankingRow(productId = 10L, likeCount = 3L, viewCount = 10L, salesCount = 5L, score = 6.6),
                ),
            ),
        )

        verify(exactly = 1) { repository.deleteAllByMonthStartDate(LocalDate.parse("2026-04-01")) }
        assertThat(saved.captured.single().ranking).isEqualTo(1L)
        assertThat(saved.captured.single().monthStartDate).isEqualTo(LocalDate.parse("2026-04-01"))
        assertThat(saved.captured.single().monthEndDate).isEqualTo(LocalDate.parse("2026-04-30"))
    }

    private fun stepExecution(): StepExecution = StepExecution("monthly", JobExecution(JobInstance(1L, "job"), org.springframework.batch.core.JobParameters()))
}
