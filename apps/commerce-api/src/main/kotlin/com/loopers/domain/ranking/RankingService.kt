package com.loopers.domain.ranking

import com.loopers.infrastructure.ranking.ProductRankMonthlyJpaRepository
import com.loopers.infrastructure.ranking.ProductRankWeeklyJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
    private val productRankWeeklyJpaRepository: ProductRankWeeklyJpaRepository,
    private val productRankMonthlyJpaRepository: ProductRankMonthlyJpaRepository,
) {
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")
        private const val KEY_PREFIX = "ranking:all"
    }

    fun getDailyRanking(date: String, page: Int, size: Int): List<RankingEntry> {
        val start = ((page - 1) * size).toLong()
        val end = start + size - 1
        return rankingRepository.getTopNWithScores(buildKey(date), start, end)
    }

    fun getDailyTotalCount(date: String): Long {
        return rankingRepository.getTotalCount(buildKey(date))
    }

    fun getWeeklyRanking(date: String, page: Int, size: Int): List<RankingEntry> {
        val monday = resolveMonday(date)
        val pageable = PageRequest.of(page - 1, size)
        return productRankWeeklyJpaRepository.findByPeriodDateOrderByRankingRankAsc(monday, pageable)
            .map { RankingEntry(productId = it.productId, score = it.totalScore) }
    }

    fun getWeeklyTotalCount(date: String): Long {
        return productRankWeeklyJpaRepository.countByPeriodDate(resolveMonday(date))
    }

    fun getMonthlyRanking(date: String, page: Int, size: Int): List<RankingEntry> {
        val yearMonth = resolveYearMonth(date)
        val pageable = PageRequest.of(page - 1, size)
        return productRankMonthlyJpaRepository.findByPeriodDateOrderByRankingRankAsc(yearMonth, pageable)
            .map { RankingEntry(productId = it.productId, score = it.totalScore) }
    }

    fun getMonthlyTotalCount(date: String): Long {
        return productRankMonthlyJpaRepository.countByPeriodDate(resolveYearMonth(date))
    }

    fun getProductRank(productId: Long): Long? {
        val key = buildKey(todayDate())
        val rank = rankingRepository.getRank(key, productId)
        return rank?.plus(1)
    }

    private fun todayDate(): String = LocalDate.now().format(DATE_FORMAT)

    private fun buildKey(date: String) = "$KEY_PREFIX:$date"

    private fun resolveMonday(date: String): String {
        return LocalDate.parse(date, DATE_FORMAT).with(DayOfWeek.MONDAY).format(DATE_FORMAT)
    }

    private fun resolveYearMonth(date: String): String {
        return LocalDate.parse(date, DATE_FORMAT).format(MONTH_FORMAT)
    }
}
