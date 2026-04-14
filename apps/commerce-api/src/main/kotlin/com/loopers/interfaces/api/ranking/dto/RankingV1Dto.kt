package com.loopers.interfaces.api.ranking.dto

import com.loopers.application.ranking.RankingInfo
import com.loopers.application.ranking.RankingPageResult
import java.math.BigDecimal
import java.util.Locale

class RankingV1Dto {

    data class RankingResponse(
        val rank: Int,
        val productId: Long,
        val productName: String,
        val price: BigDecimal,
        val score: Double,
    ) {
        companion object {
            fun from(info: RankingInfo): RankingResponse = RankingResponse(
                rank = info.rank,
                productId = info.productId,
                productName = info.productName,
                price = info.price,
                score = info.score,
            )
        }
    }

    data class RankingPageResponse(
        val period: String,
        val periodKey: String,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val content: List<RankingResponse>,
    ) {
        companion object {
            fun from(result: RankingPageResult): RankingPageResponse = RankingPageResponse(
                period = result.period.name.lowercase(Locale.ROOT),
                periodKey = result.periodKey,
                page = result.page.page,
                size = result.page.size,
                totalElements = result.page.totalElements,
                content = result.page.content.map { RankingResponse.from(it) },
            )
        }
    }
}
