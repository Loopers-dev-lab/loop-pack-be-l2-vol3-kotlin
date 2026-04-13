package com.loopers.batch.job.ranking

import com.loopers.batch.ranking.entity.MvProductRankWeeklyBatchEntity
import com.loopers.batch.ranking.entity.QMvProductRankWeeklyBatchEntity
import com.loopers.batch.ranking.entity.QProductMetricsDailyBatchEntity
import com.loopers.batch.ranking.entity.WeeklyRankRow
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class WeeklyRankingQueryDao(
    private val queryFactory: JPAQueryFactory,
    @PersistenceContext private val entityManager: EntityManager,
) {
    fun selectTop100Aggregate(startDate: LocalDate, endDate: LocalDate): List<WeeklyRankRow> {
        val daily = QProductMetricsDailyBatchEntity.productMetricsDailyBatchEntity
        val scoreExpr = daily.viewCount.sum().doubleValue().multiply(0.1)
            .add(daily.likeCount.sum().doubleValue().multiply(0.2))
            .add(daily.salesCount.sum().doubleValue().multiply(0.7))

        return queryFactory
            .select(daily.productId, daily.viewCount.sum(), daily.likeCount.sum(), daily.salesCount.sum(), scoreExpr)
            .from(daily)
            .where(daily.metricDate.between(startDate, endDate))
            .groupBy(daily.productId)
            .having(scoreExpr.gt(0.0))
            .orderBy(scoreExpr.desc(), daily.productId.asc())
            .limit(100)
            .fetch()
            .map { tuple ->
                WeeklyRankRow(
                    productId = tuple.get(daily.productId)!!,
                    totalViewCount = tuple.get(daily.viewCount.sum())!!,
                    totalLikeCount = tuple.get(daily.likeCount.sum())!!,
                    totalSalesCount = tuple.get(daily.salesCount.sum())!!,
                    score = tuple.get(scoreExpr)!!,
                )
            }
    }

    fun deleteByPeriodKey(periodKey: String) {
        val mv = QMvProductRankWeeklyBatchEntity.mvProductRankWeeklyBatchEntity
        queryFactory.delete(mv).where(mv.periodKey.eq(periodKey)).execute()
    }

    fun bulkInsert(periodKey: String, rows: List<WeeklyRankRow>, startDate: LocalDate, endDate: LocalDate) {
        rows.forEachIndexed { index, row ->
            val entity = MvProductRankWeeklyBatchEntity(
                rankNo = index + 1,
                productId = row.productId,
                score = row.score,
                viewCount = row.totalViewCount,
                likeCount = row.totalLikeCount,
                salesCount = row.totalSalesCount,
                periodKey = periodKey,
                periodStartDate = startDate,
                periodEndDate = endDate,
            )
            entityManager.persist(entity)
        }
    }
}
