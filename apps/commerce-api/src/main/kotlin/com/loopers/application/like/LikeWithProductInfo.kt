package com.loopers.application.like

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.like.model.Like
import java.math.BigDecimal

data class LikeWithProductInfo(
    val likeId: Long,
    val productId: Long,
    val productName: String,
    val productPrice: BigDecimal,
    val productStatus: String,
) {
    companion object {
        fun from(like: Like, product: Product): LikeWithProductInfo = LikeWithProductInfo(
            likeId = like.id,
            productId = product.id.value,
            productName = product.name,
            productPrice = product.price.value,
            productStatus = product.status.name,
        )
    }
}
