package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import java.time.ZonedDateTime

data class AdminProductInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val brandId: Long,
    val brandName: String,
    val stockQuantity: Int,
    val likeCount: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(product: Product, brand: Brand): AdminProductInfo {
            return AdminProductInfo(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price.value,
                brandId = product.brandId,
                brandName = brand.name,
                stockQuantity = product.stockQuantity.value,
                likeCount = product.likes.value,
                createdAt = product.createdAt,
                updatedAt = product.updatedAt,
            )
        }
    }
}
