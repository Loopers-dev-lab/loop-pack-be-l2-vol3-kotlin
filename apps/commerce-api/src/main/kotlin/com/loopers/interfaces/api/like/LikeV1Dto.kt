package com.loopers.interfaces.api.like

import com.loopers.application.like.LikedProductResult
import com.loopers.domain.Money
import java.time.ZonedDateTime

class LikeV1Dto {

    data class LikedProductResponse(
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
            fun from(result: LikedProductResult): LikedProductResponse {
                return LikedProductResponse(
                    likeId = result.likeId,
                    likedAt = result.likedAt,
                    productId = result.productId,
                    brandName = result.brandName,
                    productName = result.productName,
                    price = result.price,
                    likeCount = result.likeCount,
                    imageUrl = result.imageUrl,
                )
            }
        }
    }
}
