package com.loopers.application.like

import com.loopers.domain.like.Like
import java.time.ZonedDateTime

data class LikeInfo(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(like: Like): LikeInfo {
            val id = requireNotNull(like.persistenceId) {
                "Like.persistenceId가 null입니다. 저장된 Like만 매핑 가능합니다."
            }
            return LikeInfo(
                id = id,
                userId = like.userId,
                productId = like.productId,
                createdAt = like.createdAt,
            )
        }
    }
}
