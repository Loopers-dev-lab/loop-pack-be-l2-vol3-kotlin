package com.loopers.application.like

import com.loopers.application.product.ProductResult
import com.loopers.domain.Money
import com.loopers.domain.like.LikeInfo
import java.time.ZonedDateTime

data class LikedProductResult(
    val likeId: Long,
    val likedAt: ZonedDateTime,
    val productId: Long,
    val brandName: String,
    val productName: String,
    val price: Money,
    val likeCount: Int,
    val imageUrl: String?,
) {
    companion object {
        fun from(likeInfo: LikeInfo, productResult: ProductResult): LikedProductResult {
            return LikedProductResult(
                likeId = likeInfo.likeId,
                likedAt = likeInfo.likedAt,
                productId = productResult.id,
                brandName = productResult.brandName,
                productName = productResult.name,
                price = productResult.price,
                likeCount = productResult.likeCount,
                imageUrl = productResult.imageUrl,
            )
        }
    }
}
