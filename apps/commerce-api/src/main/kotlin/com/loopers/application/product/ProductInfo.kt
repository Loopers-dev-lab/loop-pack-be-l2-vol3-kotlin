package com.loopers.application.product

import com.loopers.domain.product.Product
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val description: String,
    val price: Long,
    val stock: Int,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(product: Product, brandName: String, stock: Int): ProductInfo {
            return ProductInfo(
                id = product.id,
                brandId = product.brandId,
                brandName = brandName,
                name = product.name,
                description = product.description,
                price = product.price.amount,
                stock = stock,
                imageUrl = product.imageUrl,
                likeCount = product.likeCount,
                available = !product.isDeleted() && stock > 0,
                createdAt = product.createdAt,
                updatedAt = product.updatedAt,
                deletedAt = product.deletedAt,
            )
        }
    }
}
