package com.loopers.application.like

import com.loopers.domain.like.Like
import java.time.ZonedDateTime

data class LikeInfo(
    val id: Long,
    val productId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(like: Like): LikeInfo {
            return LikeInfo(
                id = like.id,
                productId = like.productId,
                createdAt = like.createdAt,
            )
        }
    }
}
