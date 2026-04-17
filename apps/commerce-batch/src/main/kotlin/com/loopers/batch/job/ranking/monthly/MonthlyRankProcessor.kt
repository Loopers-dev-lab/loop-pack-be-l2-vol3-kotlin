package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.AggregatedProductMetric
import com.loopers.infrastructure.ranking.MonthlyProductRankEntity
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * SQL에서 이미 ORDER BY total_score DESC LIMIT 100 정렬된 결과를 받아
 * 1~100 rank 번호를 차례로 부여한다.
 *
 * Reader가 집계·정렬·TOP 컷을 끝낸 상태이므로 Processor는 순번만 매긴다.
 */
@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankJobConfig.JOB_NAME)
@Component
class MonthlyRankProcessor(
    @Value("#{jobParameters['baseDate']}") baseDateStr: String,
) : ItemProcessor<AggregatedProductMetric, MonthlyProductRankEntity> {
    private val baseDate: LocalDate = LocalDate.parse(baseDateStr)
    private val year: Int = baseDate.year
    private val month: Int = baseDate.monthValue
    private val rankCounter = AtomicInteger(0)

    override fun process(item: AggregatedProductMetric): MonthlyProductRankEntity =
        MonthlyProductRankEntity(
            productId = item.productId,
            year = year,
            month = month,
            totalScore = item.totalScore,
            rankNumber = rankCounter.incrementAndGet(),
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            unitsSold = item.unitsSold,
            salesAmount = item.salesAmount,
            orderScore = item.orderScore,
        )
}
