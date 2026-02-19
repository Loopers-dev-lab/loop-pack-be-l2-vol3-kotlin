package com.loopers.interfaces.api.like.dto

import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.like.entity.Like
import java.math.BigDecimal

class LikeV1Dto {
    data class LikeResponse(
        val likeId: Long,
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val productStatus: Product.ProductStatus,
    ) {
        companion object {
            fun from(like: Like, product: Product): LikeResponse {
                return LikeResponse(
                    likeId = like.id,
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    productStatus = product.status,
                )
            }
        }
    }
}
