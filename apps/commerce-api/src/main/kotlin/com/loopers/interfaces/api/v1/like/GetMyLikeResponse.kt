package com.loopers.interfaces.api.v1.like

import com.loopers.application.like.LikeInfo
import java.time.ZonedDateTime

data class GetMyLikeResponse(
    val productId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(likeInfo: LikeInfo) = GetMyLikeResponse(
            productId = likeInfo.productId,
            createdAt = likeInfo.createdAt,
        )
    }
}
