package com.loopers.job.ranking

import com.loopers.batch.job.ranking.step.PrepareMonthFixedTasklet
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
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

class PrepareMonthFixedTaskletTest {
    private val repository: ProductRankMonthlyMvJpaRepository = mock()

    @Test
    fun `month-fixed scope면 월간 기간을 계산하고 기존 MV를 삭제한다`() {
        val tasklet = PrepareMonthFixedTasklet(
            scope = "month-fixed",
            targetDate = "20260417",
            productRankMonthlyMvJpaRepository = repository,
        )
        val stepExecution = MetaDataInstanceFactory.createStepExecution()

        tasklet.execute(
            StepContribution(stepExecution),
            ChunkContext(StepContext(stepExecution)),
        )

        assertThat(stepExecution.jobExecution.executionContext.get("monthPeriodStartDate")).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(stepExecution.jobExecution.executionContext.get("monthPeriodEndDate")).isEqualTo(LocalDate.of(2026, 4, 30))
        verify(repository).deleteByPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
    }

    @Test
    fun `week-fixed scope면 월간 준비 step은 no-op 한다`() {
        val tasklet = PrepareMonthFixedTasklet(
            scope = "week-fixed",
            targetDate = "20260417",
            productRankMonthlyMvJpaRepository = repository,
        )
        val stepExecution = MetaDataInstanceFactory.createStepExecution()

        tasklet.execute(
            StepContribution(stepExecution),
            ChunkContext(StepContext(stepExecution)),
        )

        assertThat(stepExecution.jobExecution.executionContext.containsKey("monthPeriodStartDate")).isFalse()
        verifyNoInteractions(repository)
    }
}
