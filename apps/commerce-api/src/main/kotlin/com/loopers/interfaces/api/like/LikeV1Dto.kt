package com.loopers.interfaces.api.like

import com.loopers.application.like.LikedProductInfo

class LikeV1Dto {
    data class LikedProductResponse(
        val productId: Long,
        val productName: String,
        val price: Long,
        val imageUrl: String,
        val brandName: String,
    ) {
        companion object {
            fun from(info: LikedProductInfo): LikedProductResponse {
                return LikedProductResponse(
                    productId = info.productId,
                    productName = info.productName,
                    price = info.price,
                    imageUrl = info.imageUrl,
                    brandName = info.brandName,
                )
            }
        }
    }
}
