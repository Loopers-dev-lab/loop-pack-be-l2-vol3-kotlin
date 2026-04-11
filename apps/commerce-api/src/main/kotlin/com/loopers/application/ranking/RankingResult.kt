package com.loopers.application.ranking

import com.loopers.domain.Money
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.ranking.RankingInfo

/**
 * 랭킹 + 상품정보가 조합된 결과.
 */
data class RankedProductResult(
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
        fun from(rankingInfo: RankingInfo, productInfo: ProductInfo, brandName: String): RankedProductResult {
            return RankedProductResult(
                rank = rankingInfo.rank,
                score = rankingInfo.score,
                productId = rankingInfo.productId,
                productName = productInfo.name,
                brandName = brandName,
                price = productInfo.price,
                imageUrl = productInfo.imageUrl,
                soldOut = productInfo.stockQuantity <= 0,
            )
        }
    }
}

data class RankingPageResult(
    val rankings: List<RankedProductResult>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
)
