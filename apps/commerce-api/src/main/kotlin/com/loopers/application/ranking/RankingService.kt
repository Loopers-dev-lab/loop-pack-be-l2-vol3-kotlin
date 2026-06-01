package com.loopers.application.ranking

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.ProductRankMvRepository
import com.loopers.domain.ranking.RankingPeriodType
import com.loopers.domain.ranking.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
    private val productRankMvRepository: ProductRankMvRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun getRankings(date: LocalDate, page: Int, size: Int, period: RankingPeriodType = RankingPeriodType.DAILY): RankingPageInfo {
        return when (period) {
            RankingPeriodType.DAILY -> getDailyRankings(date, page, size)
            RankingPeriodType.WEEKLY -> getMvRankings(date, page, size, period)
            RankingPeriodType.MONTHLY -> getMvRankings(date, page, size, period)
        }
    }

    private fun getDailyRankings(date: LocalDate, page: Int, size: Int): RankingPageInfo {
        val start = ((page - 1) * size).toLong()
        val end = start + size - 1

        val rankingScores = rankingRepository.getTopRankings(date, start, end)
        val totalCount = rankingRepository.getTotalCount(date)

        if (rankingScores.isEmpty()) {
            return RankingPageInfo(rankings = emptyList(), totalCount = totalCount)
        }

        val productIds = rankingScores.map { it.productId }
        val productMap = productRepository.findAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIdIncludingDeleted(brandIds).associateBy { it.id }

        val rankings = rankingScores
            .mapIndexedNotNull { index, score ->
                val product = productMap[score.productId] ?: return@mapIndexedNotNull null
                val brand = brandMap[product.brandId]
                RankingInfo(
                    rank = (start + index + 1).toInt(),
                    productId = product.id,
                    productName = product.name,
                    brandName = brand?.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    score = score.score,
                )
            }

        return RankingPageInfo(rankings = rankings, totalCount = totalCount)
    }

    private fun getMvRankings(date: LocalDate, page: Int, size: Int, period: RankingPeriodType): RankingPageInfo {
        val periodStartDate = period.periodStartDate(date)
        val mvRows = when (period) {
            RankingPeriodType.WEEKLY -> productRankMvRepository.findWeeklyByPeriodStartDate(periodStartDate)
            RankingPeriodType.MONTHLY -> productRankMvRepository.findMonthlyByPeriodStartDate(periodStartDate)
            else -> emptyList()
        }

        if (mvRows.isEmpty()) {
            return RankingPageInfo(rankings = emptyList(), totalCount = 0)
        }

        val start = (page - 1) * size
        val paged = mvRows.drop(start).take(size)
        val productIds = paged.map { it.productId }
        val productMap = productRepository.findAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIdIncludingDeleted(brandIds).associateBy { it.id }

        val rankings = paged.mapNotNull { row ->
            val product = productMap[row.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId]
            RankingInfo(
                rank = row.rank,
                productId = product.id,
                productName = product.name,
                brandName = brand?.name,
                price = product.price,
                imageUrl = product.imageUrl,
                score = row.totalScore,
            )
        }

        return RankingPageInfo(rankings = rankings, totalCount = mvRows.size.toLong())
    }

    fun getProductRank(productId: Long, date: LocalDate): Int? {
        val rank = rankingRepository.getProductRank(productId, date) ?: return null
        return (rank + 1).toInt()
    }
}

data class RankingPageInfo(
    val rankings: List<RankingInfo>,
    val totalCount: Long,
)
