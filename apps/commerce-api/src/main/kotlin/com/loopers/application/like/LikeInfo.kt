package com.loopers.application.like

import com.loopers.domain.product.Product

data class LikeInfo(
    val productId: Long,
    val productName: String,
    val price: Long,
    val description: String?,
    val brandId: Long,
    val likes: Int,
) {
    companion object {
        fun from(product: Product): LikeInfo {
            return LikeInfo(
                productId = product.id,
                productName = product.name,
                price = product.price.value,
                description = product.description,
                brandId = product.brandId,
                likes = product.likes.value,
            )
        }
    }
}
