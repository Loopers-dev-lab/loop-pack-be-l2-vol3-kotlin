package com.loopers.infrastructure.like

import com.loopers.domain.like.Like

object LikeMapper {

    fun toDomain(entity: LikeEntity): Like {
        val id = requireNotNull(entity.id) {
            "LikeEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Like.reconstitute(
            persistenceId = id,
            userId = entity.userId,
            productId = entity.productId,
            createdAt = entity.createdAt,
        )
    }
}
