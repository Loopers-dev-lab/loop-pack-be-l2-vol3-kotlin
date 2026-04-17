package com.loopers.batch.job.ranking

import com.loopers.infrastructure.ranking.RankingPeriodDateRangeResolver
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.JobParametersValidator
import org.springframework.stereotype.Component

@Component
class ProductRankingAggregationJobParameterValidator : JobParametersValidator {
    override fun validate(parameters: JobParameters?) {
        val targetDate = parameters?.getString(ProductRankingAggregationJobConfig.TARGET_DATE_PARAMETER)
            ?: throw JobParametersInvalidException("targetDate job parameter is required")

        runCatching { RankingPeriodDateRangeResolver.parse(targetDate) }
            .getOrElse {
                throw JobParametersInvalidException("targetDate must match yyyyMMdd")
            }
    }
}
