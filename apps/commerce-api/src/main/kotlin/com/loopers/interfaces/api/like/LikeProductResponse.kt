package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeProductInfo
import java.time.ZonedDateTime

data class LikeProductResponse(
    val productId: Long,
    val productName: String,
    val price: Long,
    val imageUrl: String,
    val available: Boolean,
    val likedAt: ZonedDateTime,
) {
    companion object {
        fun from(info: LikeProductInfo): LikeProductResponse {
            return LikeProductResponse(
                productId = info.productId,
                productName = info.productName,
                price = info.price,
                imageUrl = info.imageUrl,
                available = info.available,
                likedAt = info.likedAt,
            )
        }
    }
}
