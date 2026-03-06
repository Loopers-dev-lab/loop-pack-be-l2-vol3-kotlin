package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeInfo

class LikeV1Dto {
    data class LikeProductResponse(
        val productId: Long,
        val productName: String,
        val brandName: String,
        val price: Long,
        val likeCount: Int,
    ) {
        companion object {
            fun from(likeInfo: LikeInfo): LikeProductResponse {
                return LikeProductResponse(
                    productId = likeInfo.productInfo.id,
                    productName = likeInfo.productInfo.name,
                    brandName = likeInfo.productInfo.brand.name,
                    price = likeInfo.productInfo.price,
                    likeCount = likeInfo.productInfo.likeCount,
                )
            }
        }
    }
}
