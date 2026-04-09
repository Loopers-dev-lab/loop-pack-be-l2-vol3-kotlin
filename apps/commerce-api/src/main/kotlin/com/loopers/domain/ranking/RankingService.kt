package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {

    fun getTopRankings(date: LocalDate, page: Int, size: Int): List<RankingEntry> {
        val offset = ((page - 1) * size).toLong()
        return rankingRepository.getTopRankings(date, offset, size.toLong())
    }

    fun getRank(date: LocalDate, productId: Long): Long? {
        return rankingRepository.getRank(date, productId)
    }

    fun carryOver(fromDate: LocalDate, toDate: LocalDate, weight: Double) {
        rankingRepository.carryOver(fromDate, toDate, weight)
    }
}
