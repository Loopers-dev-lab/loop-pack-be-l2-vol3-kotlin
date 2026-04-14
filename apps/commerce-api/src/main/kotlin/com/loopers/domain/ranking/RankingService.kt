package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val KEY_PREFIX = "ranking:all"
    }

    fun getRanking(date: String, page: Int, size: Int): List<RankingEntry> {
        val start = ((page - 1) * size).toLong()
        val end = start + size - 1
        return rankingRepository.getTopNWithScores(buildKey(date), start, end)
    }

    fun getTotalCount(date: String): Long {
        return rankingRepository.getTotalCount(buildKey(date))
    }

    fun getProductRank(productId: Long): Long? {
        val key = buildKey(todayDate())
        val rank = rankingRepository.getRank(key, productId)
        return rank?.plus(1)
    }

    private fun todayDate(): String = LocalDate.now().format(DATE_FORMAT)

    private fun buildKey(date: String) = "$KEY_PREFIX:$date"
}
