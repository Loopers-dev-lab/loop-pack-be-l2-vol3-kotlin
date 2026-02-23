package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeInfo

class LikeDto {
    data class UserLikeResponse(
        val productId: Long,
        val productName: String,
        val price: Long,
        val description: String?,
        val brandId: Long,
        val likes: Int,
    ) {
        companion object {
            fun from(info: LikeInfo): UserLikeResponse {
                return UserLikeResponse(
                    productId = info.productId,
                    productName = info.productName,
                    price = info.price,
                    description = info.description,
                    brandId = info.brandId,
                    likes = info.likes,
                )
            }
        }
    }
}
