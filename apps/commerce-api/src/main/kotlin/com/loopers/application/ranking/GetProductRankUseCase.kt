package com.loopers.application.ranking

import com.loopers.common.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GetProductRankUseCase(
    private val rankingStore: RankingStore,
    private val weeklyRankReader: WeeklyRankReader,
    private val monthlyRankReader: MonthlyRankReader,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(productId: Long): ProductRanks = ProductRanks(
        daily = dailyRank(productId),
        weekly = weeklyRank(productId),
        monthly = monthlyRank(productId),
    )

    private fun dailyRank(productId: Long): Int? = try {
        rankingStore.getProductRank(DateUtils.todayKst(), productId)?.let { (it + 1).toInt() }
    } catch (e: Exception) {
        log.warn("daily rank 조회 실패 [productId={}]", productId, e)
        null
    }

    private fun weeklyRank(productId: Long): Int? = try {
        weeklyRankReader.findLatestRankOfProduct(productId)
    } catch (e: Exception) {
        log.warn("weekly rank 조회 실패 [productId={}]", productId, e)
        null
    }

    private fun monthlyRank(productId: Long): Int? = try {
        monthlyRankReader.findLatestRankOfProduct(productId)
    } catch (e: Exception) {
        log.warn("monthly rank 조회 실패 [productId={}]", productId, e)
        null
    }
}
