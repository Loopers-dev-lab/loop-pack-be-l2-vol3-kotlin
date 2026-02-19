package com.loopers.application.like

import com.loopers.domain.catalog.product.Product
import com.loopers.domain.like.Like
import java.math.BigDecimal

data class LikeInfo(
    val likeId: Long,
    val productId: Long,
    val productName: String,
    val productPrice: BigDecimal,
    val productStatus: Product.ProductStatus,
) {
    companion object {
        fun from(like: Like, product: Product): LikeInfo {
            return LikeInfo(
                likeId = like.id,
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                productStatus = product.status,
            )
        }
    }
}
