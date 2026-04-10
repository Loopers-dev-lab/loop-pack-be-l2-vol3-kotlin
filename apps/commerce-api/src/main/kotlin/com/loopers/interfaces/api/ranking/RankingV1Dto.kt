package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.GetRankingResult
import com.loopers.application.ranking.RankedProductResult
import java.math.BigDecimal
import java.time.LocalDate

class RankingV1Dto {
    data class RankedProductResponse(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val name: String,
        val price: BigDecimal,
        val brandId: Long,
        val brandName: String,
    ) {
        companion object {
            fun from(result: RankedProductResult): RankedProductResponse {
                return RankedProductResponse(
                    rank = result.rank,
                    score = result.score,
                    productId = result.productId,
                    name = result.name,
                    price = result.price,
                    brandId = result.brandId,
                    brandName = result.brandName,
                )
            }
        }
    }

    data class RankingPageResponse(
        val date: LocalDate,
        val page: Int,
        val size: Int,
        val totalCount: Long,
        val hasNext: Boolean,
        val items: List<RankedProductResponse>,
    ) {
        companion object {
            fun from(result: GetRankingResult): RankingPageResponse {
                return RankingPageResponse(
                    date = result.date,
                    page = result.page,
                    size = result.size,
                    totalCount = result.totalCount,
                    hasNext = result.hasNext,
                    items = result.items.map { RankedProductResponse.from(it) },
                )
            }
        }
    }
}
