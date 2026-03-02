package com.loopers.application.like

import com.loopers.domain.like.LikeModel

data class LikeInfo(
    val id: Long,
    val userId: Long,
    val productId: Long,
) {
    companion object {
        fun from(model: LikeModel) = LikeInfo(
            id = model.id,
            userId = model.userId,
            productId = model.productId,
        )
    }
}
