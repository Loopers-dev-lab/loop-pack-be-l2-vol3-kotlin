package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    fun getTopRankings(date: LocalDate, page: Int, size: Int): RankingPage {
        val key = RankingKeyGenerator.dailyKey(date)
        val offset = ((page - 1) * size).toLong()

        val entries = rankingRepository.getTopN(key, offset, size.toLong())
        val totalCount = rankingRepository.getTotalCount(key)

        return RankingPage(
            entries = entries.mapIndexed { index, entry ->
                RankedProduct(
                    rank = offset + index + 1,
                    productId = entry.productId,
                    score = entry.score,
                )
            },
            totalElements = totalCount,
            page = page,
            size = size,
        )
    }

    fun getProductRank(date: LocalDate, productId: Long): Long? {
        val key = RankingKeyGenerator.dailyKey(date)
        return rankingRepository.getRank(key, productId)?.let { it + 1 }
    }
}

data class RankedProduct(
    val rank: Long,
    val productId: Long,
    val score: Double,
)

data class RankingPage(
    val entries: List<RankedProduct>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
)
