package com.loopers.application.product

import com.loopers.domain.product.Product

data class ProductInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val likes: Int,
    val stockQuantity: Int,
    val brandId: Long,
) {
    companion object {
        fun from(product: Product): ProductInfo {
            return ProductInfo(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                likes = product.likes,
                stockQuantity = product.stockQuantity,
                brandId = product.brandId,
            )
        }
    }
}
