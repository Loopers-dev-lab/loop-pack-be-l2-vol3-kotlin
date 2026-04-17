package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.WeeklyAggregationRow
import com.loopers.batch.job.ranking.WeeklyRankingAggregationJobConfig
import com.loopers.domain.mv.WeeklyPeriod
import com.loopers.domain.mv.WeeklyProductRankModel
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
 * MV 엔티티로 변환한다.
 *
 * - `@StepScope` 로 Step 실행마다 새 인스턴스가 생성되므로 `rankCounter` 가 안전하게 리셋된다.
 * - Reader 가 `ORDER BY score DESC, product_id ASC` 로 내려주는 순서를 신뢰한다 (단일 스레드 step 전제).
 */
@StepScope
@ConditionalOnProperty(
    name = ["spring.batch.job.name"],
    havingValue = WeeklyRankingAggregationJobConfig.JOB_NAME,
)
@Component
class WeeklyRankingProcessor(
    @param:Value("#{jobParameters['requestDate']}") private val requestDate: String,
) : ItemProcessor<WeeklyAggregationRow, WeeklyProductRankModel> {
    private val period = WeeklyPeriod.of(LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE))
    private val rankCounter = AtomicInteger(0)

    override fun process(item: WeeklyAggregationRow): WeeklyProductRankModel {
        val rank = rankCounter.incrementAndGet()
        return WeeklyProductRankModel(
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
