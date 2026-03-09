package com.loopers.application.product

import com.loopers.domain.product.Product
import java.math.BigDecimal
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val likeCount: Int,
    val description: String?,
    val imageUrl: String?,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(product: Product): ProductInfo {
            return ProductInfo(
                id = product.id,
                brandId = product.brandId,
                name = product.name,
                price = product.price,
                stock = product.stock,
                likeCount = product.likeCount,
                description = product.description,
                imageUrl = product.imageUrl,
                createdAt = product.createdAt,
                updatedAt = product.updatedAt,
            )
        }
    }
}
