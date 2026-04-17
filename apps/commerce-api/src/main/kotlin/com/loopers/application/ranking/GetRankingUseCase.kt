package com.loopers.application.ranking

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.MonthlyRankingRepository
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.WeeklyRankingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val weeklyRankingRepository: WeeklyRankingRepository,
    private val monthlyRankingRepository: MonthlyRankingRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    fun getRankingPage(date: String, period: RankingPeriod, page: Int, size: Int): RankingPageInfo {
        val offset = ((page - 1) * size).toLong()

        val (entries, totalCount) = when (period) {
            RankingPeriod.DAILY -> {
                val entries = rankingRepository.getTopRankings(date, offset, size.toLong())
                val count = rankingRepository.getTotalCount(date)
                entries to count
            }
            RankingPeriod.WEEKLY -> {
                val rankingDate = toStartOfWeek(date)
                val entries = weeklyRankingRepository.findRankings(rankingDate, page - 1, size)
                val count = weeklyRankingRepository.countByRankingDate(rankingDate)
                if (entries.isNotEmpty()) {
                    entries to count
                } else {
                    val prevWeek = rankingDate.minusWeeks(1)
                    val fallbackEntries = weeklyRankingRepository.findRankings(prevWeek, page - 1, size)
                    val fallbackCount = weeklyRankingRepository.countByRankingDate(prevWeek)
                    if (fallbackEntries.isNotEmpty()) {
                        fallbackEntries to fallbackCount
                    } else {
                        rankingRepository.getTopRankings(date, offset, size.toLong()) to
                            rankingRepository.getTotalCount(date)
                    }
                }
            }
            RankingPeriod.MONTHLY -> {
                val rankingDate = toStartOfMonth(date)
                val entries = monthlyRankingRepository.findRankings(rankingDate, page - 1, size)
                val count = monthlyRankingRepository.countByRankingDate(rankingDate)
                if (entries.isNotEmpty()) {
                    entries to count
                } else {
                    val prevMonth = rankingDate.minusMonths(1)
                    val fallbackEntries = monthlyRankingRepository.findRankings(prevMonth, page - 1, size)
                    val fallbackCount = monthlyRankingRepository.countByRankingDate(prevMonth)
                    if (fallbackEntries.isNotEmpty()) {
                        fallbackEntries to fallbackCount
                    } else {
                        rankingRepository.getTopRankings(date, offset, size.toLong()) to
                            rankingRepository.getTotalCount(date)
                    }
                }
            }
        }

        val isRankZeroBased = period == RankingPeriod.DAILY
        return buildRankingPageInfo(entries, totalCount, page, size, isRankZeroBased)
    }

    fun getProductRank(date: String, productId: Long): ProductRankInfo? {
        val rank = rankingRepository.getRank(date, productId) ?: return null
        val score = rankingRepository.getScore(date, productId) ?: return null
        return ProductRankInfo(
            rank = rank + 1,
            score = score,
        )
    }

    private fun buildRankingPageInfo(
        entries: List<RankingEntry>,
        totalCount: Long,
        page: Int,
        size: Int,
        isRankZeroBased: Boolean,
    ): RankingPageInfo {
        if (entries.isEmpty()) {
            return RankingPageInfo(
                rankings = emptyList(),
                totalCount = totalCount,
                page = page,
                size = size,
            )
        }

        val productIds = entries.map { it.productId }
        val products = productRepository.findAllByIds(productIds)
        val productMap = products.associateBy { requireNotNull(it.persistenceId) }

        val brandIds = products.map { it.refBrandId }.toSet()
        val brandMap = brandRepository.findAllByIds(brandIds)
            .associateBy { requireNotNull(it.persistenceId) }

        val rankings = entries.mapNotNull { entry ->
            val product = productMap[entry.productId] ?: return@mapNotNull null
            val brand = brandMap[product.refBrandId]
            val displayRank = if (isRankZeroBased) entry.rank + 1 else entry.rank
            RankingInfo(
                rank = displayRank,
                score = entry.score,
                productId = entry.productId,
                productName = product.name.value,
                price = product.price.amount,
                thumbnailUrl = product.thumbnailUrl,
                brandName = brand?.name?.value ?: "Unknown",
                likeCount = product.likeCount,
            )
        }

        return RankingPageInfo(
            rankings = rankings,
            totalCount = totalCount,
            page = page,
            size = size,
        )
    }

    private fun toStartOfWeek(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DATE_FORMATTER).with(DayOfWeek.MONDAY)
    }

    private fun toStartOfMonth(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DATE_FORMATTER).with(TemporalAdjusters.firstDayOfMonth())
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
