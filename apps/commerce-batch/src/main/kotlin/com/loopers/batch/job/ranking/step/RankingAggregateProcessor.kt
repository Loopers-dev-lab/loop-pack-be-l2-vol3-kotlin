package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.MetricsAggregateDto
import com.loopers.domain.ranking.RankingAggregateTemp
import com.loopers.domain.ranking.RankingScoreCalculator
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 가중합산 score 계산 Processor.
 *
 * Reader가 조회한 메트릭 합계 DTO를 받아 score를 계산하고 중간 테이블 엔티티로 변환한다.
 * 가중치는 JobParameters로 주입받는다 (미지정 시 기본값).
 */
@StepScope
@Component
class RankingAggregateProcessor(
    @param:Value("#{jobParameters['periodType']}") private val periodType: String,
    @param:Value("#{jobParameters['periodKey']}") private val periodKey: String,
    @param:Value("#{jobParameters['viewWeight'] ?: 0.1}") private val viewWeight: Double,
    @param:Value("#{jobParameters['likeWeight'] ?: 0.2}") private val likeWeight: Double,
    @param:Value("#{jobParameters['orderWeight'] ?: 0.7}") private val orderWeight: Double,
) : ItemProcessor<MetricsAggregateDto, RankingAggregateTemp> {

    override fun process(item: MetricsAggregateDto): RankingAggregateTemp {
        val score = RankingScoreCalculator.calculate(
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            orderCount = item.orderCount,
            viewWeight = viewWeight,
            likeWeight = likeWeight,
            orderWeight = orderWeight,
        )

        return RankingAggregateTemp(
            productId = item.productId,
            score = score,
            likeCount = item.likeCount,
            orderCount = item.orderCount,
            viewCount = item.viewCount,
            periodType = periodType,
            periodKey = periodKey,
        )
    }
}
