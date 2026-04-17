package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.MonthlyAggregationRow
import com.loopers.batch.job.ranking.MonthlyRankingAggregationJobConfig
import com.loopers.domain.mv.MonthlyPeriod
import com.loopers.domain.mv.MonthlyProductRankModel
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reader 가 내려준 aggregation row 에 1부터 증가하는 `rank_position` 을 부여해
 * 월간 MV 엔티티로 변환한다.
 */
@StepScope
@ConditionalOnProperty(
    name = ["spring.batch.job.name"],
    havingValue = MonthlyRankingAggregationJobConfig.JOB_NAME,
)
@Component
class MonthlyRankingProcessor(
    @param:Value("#{jobParameters['requestDate']}") private val requestDate: String,
) : ItemProcessor<MonthlyAggregationRow, MonthlyProductRankModel> {
    private val period = MonthlyPeriod.of(LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE))
    private val rankCounter = AtomicInteger(0)

    override fun process(item: MonthlyAggregationRow): MonthlyProductRankModel {
        val rank = rankCounter.incrementAndGet()
        return MonthlyProductRankModel(
            yearMonthVal = period.yearMonthVal,
            periodStart = period.start,
            periodEnd = period.end,
            rankPosition = rank,
            productId = item.productId,
            likesCount = item.totalLikes,
            viewsCount = item.totalViews,
            salesCount = item.totalSales,
            score = item.score,
        )
    }
}
