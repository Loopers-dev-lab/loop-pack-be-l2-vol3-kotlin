package com.loopers.batch.job.ranking

import com.loopers.batch.ranking.entity.MvProductRankMonthlyBatchEntity
import com.loopers.batch.ranking.entity.MonthlyRankRow
import com.loopers.batch.ranking.entity.QMvProductRankMonthlyBatchEntity
import com.loopers.batch.ranking.entity.QProductMetricsDailyBatchEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MonthlyRankingQueryDao(
    private val queryFactory: JPAQueryFactory,
    @PersistenceContext private val entityManager: EntityManager,
) {
    fun selectTop100Aggregate(startDate: LocalDate, endDate: LocalDate): List<MonthlyRankRow> {
        val daily = QProductMetricsDailyBatchEntity.productMetricsDailyBatchEntity
        val viewSum = daily.viewCount.sum()
        val likeSum = daily.likeCount.sum()
        val salesSum = daily.salesCount.sum()
        val scoreExpr = viewSum.doubleValue().multiply(0.1)
            .add(likeSum.doubleValue().multiply(0.2))
            .add(salesSum.doubleValue().multiply(0.7))

        return queryFactory
            .select(daily.productId, viewSum, likeSum, salesSum, scoreExpr)
            .from(daily)
            .where(daily.metricDate.between(startDate, endDate))
            .groupBy(daily.productId)
            .having(scoreExpr.gt(0.0))
            .orderBy(scoreExpr.desc(), daily.productId.asc())
            .limit(100)
            .fetch()
            .map { tuple ->
                MonthlyRankRow(
                    productId = tuple.get(daily.productId)!!,
                    totalViewCount = tuple.get(viewSum)!!,
                    totalLikeCount = tuple.get(likeSum)!!,
                    totalSalesCount = tuple.get(salesSum)!!,
                    score = tuple.get(scoreExpr)!!,
                )
            }
    }

    fun deleteByPeriodKey(periodKey: String) {
        val mv = QMvProductRankMonthlyBatchEntity.mvProductRankMonthlyBatchEntity
        queryFactory.delete(mv).where(mv.periodKey.eq(periodKey)).execute()
    }

    fun bulkInsert(periodKey: String, rows: List<MonthlyRankRow>, startDate: LocalDate, endDate: LocalDate) {
        rows.forEachIndexed { index, row ->
            val entity = MvProductRankMonthlyBatchEntity(
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
