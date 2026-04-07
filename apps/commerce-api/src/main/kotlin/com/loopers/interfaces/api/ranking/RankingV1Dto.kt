package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingInfo
import com.loopers.application.ranking.RankingPageInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

class RankingV1Dto {

    @Schema(description = "랭킹 페이지 응답")
    data class RankingPageResponse(
        @Schema(description = "랭킹 목록")
        val rankings: List<RankingResponse>,
        @Schema(description = "조회 날짜", example = "20260408")
        val date: String,
        @Schema(description = "페이지 번호", example = "1")
        val page: Int,
        @Schema(description = "페이지 크기", example = "20")
        val size: Int,
        @Schema(description = "전체 랭킹 수", example = "100")
        val totalCount: Long,
    ) {
        companion object {
            fun from(pageInfo: RankingPageInfo, date: String, page: Int, size: Int): RankingPageResponse {
                return RankingPageResponse(
                    rankings = pageInfo.rankings.map { RankingResponse.from(it) },
                    date = date,
                    page = page,
                    size = size,
                    totalCount = pageInfo.totalCount,
                )
            }
        }
    }

    @Schema(description = "개별 랭킹 응답")
    data class RankingResponse(
        @Schema(description = "순위", example = "1")
        val rank: Int,
        @Schema(description = "상품 ID", example = "101")
        val productId: Long,
        @Schema(description = "상품명", example = "에어맥스 90")
        val productName: String,
        @Schema(description = "브랜드명", example = "나이키")
        val brandName: String?,
        @Schema(description = "가격", example = "129000")
        val price: BigDecimal,
        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        val imageUrl: String?,
        @Schema(description = "랭킹 점수", example = "85.5")
        val score: Double,
    ) {
        companion object {
            fun from(info: RankingInfo): RankingResponse {
                return RankingResponse(
                    rank = info.rank,
                    productId = info.productId,
                    productName = info.productName,
                    brandName = info.brandName,
                    price = info.price,
                    imageUrl = info.imageUrl,
                    score = info.score,
                )
            }
        }
    }
}
