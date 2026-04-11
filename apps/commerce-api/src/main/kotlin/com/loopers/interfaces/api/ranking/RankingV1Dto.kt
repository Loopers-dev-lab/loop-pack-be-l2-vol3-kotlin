package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankedProductResult
import com.loopers.application.ranking.RankingPageResult
import com.loopers.domain.Money
import com.loopers.domain.ranking.RankingWindow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RankingV1Dto {

    data class GetRankingsRequest(
        val window: String? = null,
        val date: String? = null,
        val hour: String? = null,
        val page: Int = 0,
        val size: Int = 20,
    ) {
        companion object {
            private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
            private val HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH")
        }

        fun resolveWindow(): RankingWindow = RankingWindow.from(window)

        /**
         * window 파라미터에 맞춰 적절한 windowKey를 생성한다.
         * - DAILY: date 파라미터 우선, 없으면 오늘 날짜
         * - HOURLY: hour 파라미터 우선, 없으면 현재 시각
         */
        fun resolveWindowKey(): String {
            val now = LocalDateTime.now()
            return when (resolveWindow()) {
                RankingWindow.DAILY -> date ?: now.format(DATE_FORMAT)
                RankingWindow.HOURLY -> hour ?: now.format(HOUR_FORMAT)
            }
        }
    }

    data class RankedProductResponse(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val productName: String,
        val brandName: String,
        val price: Money,
        val imageUrl: String?,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(result: RankedProductResult): RankedProductResponse {
                return RankedProductResponse(
                    rank = result.rank,
                    score = result.score,
                    productId = result.productId,
                    productName = result.productName,
                    brandName = result.brandName,
                    price = result.price,
                    imageUrl = result.imageUrl,
                    soldOut = result.soldOut,
                )
            }
        }
    }

    data class RankingPageResponse(
        val rankings: List<RankedProductResponse>,
        val totalCount: Long,
        val page: Int,
        val size: Int,
    ) {
        companion object {
            fun from(result: RankingPageResult): RankingPageResponse {
                return RankingPageResponse(
                    rankings = result.rankings.map { RankedProductResponse.from(it) },
                    totalCount = result.totalCount,
                    page = result.page,
                    size = result.size,
                )
            }
        }
    }
}
