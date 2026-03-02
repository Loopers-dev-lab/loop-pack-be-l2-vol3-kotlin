package com.loopers.application.like

import com.loopers.domain.product.Product
import java.time.ZonedDateTime

data class LikeProductInfo(
    val productId: Long,
    val productName: String,
    val price: Long,
    val imageUrl: String,
    val available: Boolean,
    val likedAt: ZonedDateTime,
) {
    companion object {
        fun from(product: Product, likedAt: ZonedDateTime): LikeProductInfo {
            return LikeProductInfo(
                productId = product.id,
                productName = product.name,
                price = product.price.amount,
                imageUrl = product.imageUrl,
                available = product.isAvailable(),
                likedAt = likedAt,
            )
        }
    }
}
