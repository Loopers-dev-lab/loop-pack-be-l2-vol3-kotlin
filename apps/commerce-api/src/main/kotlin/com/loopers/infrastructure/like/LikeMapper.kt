package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import org.springframework.stereotype.Component

@Component
class LikeMapper {

    fun toDomain(entity: LikeEntity): Like {
        return Like(
            id = entity.id,
            memberId = entity.memberId,
            productId = entity.productId,
        )
    }

    fun toEntity(domain: Like): LikeEntity {
        return LikeEntity(
            memberId = domain.memberId,
            productId = domain.productId,
        )
    }
}
