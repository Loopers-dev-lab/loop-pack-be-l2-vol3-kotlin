package com.loopers.batch.job.ranking

import com.loopers.infrastructure.ranking.WeeklyProductRankingJpaRepository
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

class WeeklyProductRankingItemWriterTest {
    private val repository = mockk<WeeklyProductRankingJpaRepository>(relaxed = true)
    private val writer = WeeklyProductRankingItemWriter(repository, "20260416")

    @Test
    fun `주간_writer는_기존_주간_MV를_삭제하고_연속된_rank를_저장한다`() {
        val saved = slot<List<com.loopers.infrastructure.ranking.WeeklyProductRankingEntity>>()
        every { repository.saveAll(capture(saved)) } answers { saved.captured }

        writer.beforeStep(stepExecution())
        writer.write(
            Chunk(
                listOf(
                    AggregatedProductRankingRow(productId = 10L, likeCount = 3L, viewCount = 10L, salesCount = 5L, score = 6.6),
                    AggregatedProductRankingRow(productId = 20L, likeCount = 1L, viewCount = 5L, salesCount = 4L, score = 4.7),
                ),
            ),
        )

        verify(exactly = 1) { repository.deleteAllByWeekStartDate(java.time.LocalDate.parse("2026-04-13")) }
        assertThat(saved.captured.map { it.ranking }).containsExactly(1L, 2L)
        assertThat(saved.captured.map { it.productId }).containsExactly(10L, 20L)
        assertThat(saved.captured.map { it.weekStartDate }).containsOnly(java.time.LocalDate.parse("2026-04-13"))
    }

    private fun stepExecution(): StepExecution = StepExecution("weekly", JobExecution(JobInstance(1L, "job"), org.springframework.batch.core.JobParameters()))
}
