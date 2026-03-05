package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLike
import org.springframework.stereotype.Component

@Component
class ProductLikeMapper {
    fun toDomain(entity: ProductLikeEntity): ProductLike =
        ProductLike.retrieve(
            id = entity.id!!,
            userId = entity.userId,
            productId = entity.productId,
        )

    fun toEntity(productLike: ProductLike): ProductLikeEntity =
        ProductLikeEntity(
            id = productLike.id,
            userId = productLike.userId,
            productId = productLike.productId,
        )
}
