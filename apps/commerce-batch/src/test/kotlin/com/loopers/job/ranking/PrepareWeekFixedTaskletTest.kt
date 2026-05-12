package com.loopers.job.ranking

import com.loopers.batch.job.ranking.step.PrepareWeekFixedTasklet
import com.loopers.infrastructure.ranking.ProductRankWeeklyMvJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.test.MetaDataInstanceFactory
import java.time.LocalDate

class PrepareWeekFixedTaskletTest {
    private val repository: ProductRankWeeklyMvJpaRepository = mock()

    @Test
    fun `week-fixed scope면 주간 기간을 계산하고 기존 MV를 삭제한다`() {
        val tasklet = PrepareWeekFixedTasklet(
            scope = "week-fixed",
            targetDate = "20260417",
            productRankWeeklyMvJpaRepository = repository,
        )
        val stepExecution = MetaDataInstanceFactory.createStepExecution()

        tasklet.execute(
            StepContribution(stepExecution),
            ChunkContext(StepContext(stepExecution)),
        )

        assertThat(stepExecution.jobExecution.executionContext.get("weekPeriodStartDate")).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(stepExecution.jobExecution.executionContext.get("weekPeriodEndDate")).isEqualTo(LocalDate.of(2026, 4, 19))
        verify(repository).deleteByPeriod(LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 19))
    }

    @Test
    fun `month-fixed scope면 주간 준비 step은 no-op 한다`() {
        val tasklet = PrepareWeekFixedTasklet(
            scope = "month-fixed",
            targetDate = "20260417",
            productRankWeeklyMvJpaRepository = repository,
        )
        val stepExecution = MetaDataInstanceFactory.createStepExecution()

        tasklet.execute(
            StepContribution(stepExecution),
            ChunkContext(StepContext(stepExecution)),
        )

        assertThat(stepExecution.jobExecution.executionContext.containsKey("weekPeriodStartDate")).isFalse()
        verifyNoInteractions(repository)
    }
}
