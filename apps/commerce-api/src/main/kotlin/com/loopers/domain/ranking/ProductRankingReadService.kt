package com.loopers.domain.ranking

import com.loopers.interfaces.api.ranking.RankingPeriod
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields

@Service
class ProductRankingReadService(
    private val productRankingRepository: ProductRankingRepository,
    private val mvProductRankRepository: MvProductRankRepository,
) {

    companion object {
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun getRank(processingDate: LocalDate?, productId: Long): Long? {
        return productRankingRepository.getRank(resolveProcessingDate(processingDate), productId)
    }

    fun getRankedProductsWithCount(
        processingDate: LocalDate?,
        page: Int,
        size: Int,
        period: RankingPeriod = RankingPeriod.DAILY,
    ): RankedProductsWithCount {
        return when (period) {
            RankingPeriod.DAILY -> {
                productRankingRepository.getRankedProductsWithCount(resolveProcessingDate(processingDate), page, size)
            }
            RankingPeriod.WEEKLY -> {
                val yearWeek = getYearWeek(resolveProcessingDate(processingDate))
                val products = mvProductRankRepository.findWeeklyRanking(yearWeek, page, size)
                val count = mvProductRankRepository.countWeekly(yearWeek)
                RankedProductsWithCount(products, count)
            }
            RankingPeriod.MONTHLY -> {
                val yearMonth = getYearMonth(resolveProcessingDate(processingDate))
                val products = mvProductRankRepository.findMonthlyRanking(yearMonth, page, size)
                val count = mvProductRankRepository.countMonthly(yearMonth)
                RankedProductsWithCount(products, count)
            }
        }
    }

    private fun resolveProcessingDate(processingDate: LocalDate?): LocalDate {
        return processingDate ?: LocalDate.now(KST_ZONE_ID)
    }

    private fun getYearWeek(date: LocalDate): String {
        val year = date.year
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format("%d-W%02d", year, week)
    }

    private fun getYearMonth(date: LocalDate): String {
        return String.format("%d-%02d", date.year, date.monthValue)
    }
}
