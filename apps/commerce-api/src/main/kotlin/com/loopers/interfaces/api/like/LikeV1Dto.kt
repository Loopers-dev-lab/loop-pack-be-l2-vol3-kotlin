package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeInfo

class LikeV1Dto {

    data class RegisterRequest(val productId: Long)

    data class RegisteredResponse(
        val id: Long,
        val memberId: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(info: LikeInfo.Registered) = RegisteredResponse(
                id = info.id,
                memberId = info.memberId,
                productId = info.productId,
            )
        }
    }

    data class DetailResponse(
        val id: Long,
        val productId: Long,
        val productName: String,
        val price: Long,
        val brandName: String,
    ) {
        companion object {
            fun from(info: LikeInfo.Detail) = DetailResponse(
                id = info.id,
                productId = info.productId,
                productName = info.productName,
                price = info.price,
                brandName = info.brandName,
            )
        }
    }
}
