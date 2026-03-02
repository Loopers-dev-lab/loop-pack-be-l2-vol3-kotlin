package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeInfo

class LikeV1Dto {

    data class LikeResponse(
        val id: Long,
        val userId: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(info: LikeInfo) = LikeResponse(
                id = info.id,
                userId = info.userId,
                productId = info.productId,
            )
        }
    }
}
