package com.loopers.batch.job.ranking

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersInvalidException

class ProductRankingAggregationJobParameterValidatorTest {
    private val validator = ProductRankingAggregationJobParameterValidator()

    @Test
    fun `targetDate가_없으면_예외가_발생한다`() {
        assertThatThrownBy { validator.validate(JobParametersBuilder().toJobParameters()) }
            .isInstanceOf(JobParametersInvalidException::class.java)
            .hasMessageContaining("targetDate")
    }

    @Test
    fun `targetDate_형식이_잘못되면_예외가_발생한다`() {
        assertThatThrownBy {
            validator.validate(
                JobParametersBuilder()
                    .addString(ProductRankingAggregationJobConfig.TARGET_DATE_PARAMETER, "2026-04-16")
                    .toJobParameters(),
            )
        }.isInstanceOf(JobParametersInvalidException::class.java)
            .hasMessageContaining("yyyyMMdd")
    }
}
