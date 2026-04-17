package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.AggregatedProductMetric
import com.loopers.infrastructure.ranking.MonthlyProductRankEntity
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * SQL에서 이미 ROW_NUMBER()로 rank 번호까지 부여된 결과를 받아 MV Entity로 변환한다.
 *
 * Reader가 집계·정렬·TOP 컷·rank 부여를 끝낸 상태이므로 Processor는 필드 매핑만 담당한다.
 * restart 시 Processor 내부 카운터에 의존하지 않아 멱등성과 병렬성이 보장된다.
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

    override fun process(item: AggregatedProductMetric): MonthlyProductRankEntity =
        MonthlyProductRankEntity(
            productId = item.productId,
            year = year,
            month = month,
            totalScore = item.totalScore,
            rankNumber = item.rankNumber,
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            unitsSold = item.unitsSold,
            salesAmount = item.salesAmount,
            orderScore = item.orderScore,
        )
}
