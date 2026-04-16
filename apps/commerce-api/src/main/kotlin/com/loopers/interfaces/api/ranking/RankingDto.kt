package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingInfo
import com.loopers.application.ranking.RankingResult
import com.loopers.domain.ranking.Period

class RankingDto {
    data class Response(
        val period: Period,
        val periodStart: String,
        val periodEnd: String,
        val items: List<ItemResponse>,
    ) {
        companion object {
            fun from(result: RankingResult): Response {
                return Response(
                    period = result.period,
                    periodStart = result.periodStart,
                    periodEnd = result.periodEnd,
                    items = result.items.map { ItemResponse.from(it) },
                )
            }
        }
    }

    data class ItemResponse(
        val productId: Long,
        val rank: Long,
        val score: Double,
        val productName: String,
        val productPrice: Long,
    ) {
        companion object {
            fun from(info: RankingInfo): ItemResponse {
                return ItemResponse(
                    productId = info.productId,
                    rank = info.rank,
                    score = info.score,
                    productName = info.productName,
                    productPrice = info.productPrice,
                )
            }
        }
    }
}
