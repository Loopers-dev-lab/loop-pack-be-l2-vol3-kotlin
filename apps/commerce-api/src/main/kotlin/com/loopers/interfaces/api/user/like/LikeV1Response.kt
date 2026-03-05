package com.loopers.interfaces.api.user.like

import com.loopers.application.user.like.UserProductLikeResult
import java.math.BigDecimal

class LikeV1Response {
    data class LikedProduct(
        val productId: Long,
        val productName: String,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val thumbnailUrl: String?,
        val likeCount: Int,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(result: UserProductLikeResult.LikedProduct): LikedProduct = LikedProduct(
                productId = result.productId,
                productName = result.productName,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
                soldOut = result.soldOut,
            )
        }
    }
}
