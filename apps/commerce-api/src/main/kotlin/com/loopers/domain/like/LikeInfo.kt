package com.loopers.domain.like

import java.time.ZonedDateTime

data class LikeInfo(
    val likeId: Long,
    val userId: Long,
    val productId: Long,
    val likedAt: ZonedDateTime,
) {
    companion object {
        fun from(like: Like): LikeInfo = LikeInfo(
            likeId = like.id,
            userId = like.userId,
            productId = like.productId,
            likedAt = like.createdAt,
        )
    }
}
