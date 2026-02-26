package com.loopers.domain.product

import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val likeCount: Int,
    val status: ProductStatus,
    val displayYn: Boolean,
    val imageUrl: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(product: Product): ProductInfo {
            return ProductInfo(
                id = product.id,
                brandId = product.brandId,
                name = product.name,
                description = product.description,
                price = product.price,
                stockQuantity = product.stockQuantity,
                likeCount = product.likeCount,
                status = product.status,
                displayYn = product.displayYn,
                imageUrl = product.imageUrl,
                createdAt = product.createdAt,
            )
        }
    }
}
