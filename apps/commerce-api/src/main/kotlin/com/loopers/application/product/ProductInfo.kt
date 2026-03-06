package com.loopers.application.product

import com.loopers.domain.product.ProductModel
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val brandName: String?,
    val name: String,
    val description: String,
    val price: Long,
    val stockQuantity: Int,
    val likeCount: Int,
    val imageUrl: String,
    val status: String,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    val soldOut: Boolean get() = stockQuantity == 0

    companion object {
        fun from(model: ProductModel, brandName: String? = null): ProductInfo {
            return ProductInfo(
                id = model.id,
                brandId = model.brandId,
                brandName = brandName,
                name = model.name,
                description = model.description,
                price = model.price,
                stockQuantity = model.stockQuantity,
                likeCount = model.likeCount,
                imageUrl = model.imageUrl,
                status = model.status.name,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
            )
        }
    }
}
