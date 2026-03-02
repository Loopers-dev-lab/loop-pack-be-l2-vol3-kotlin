package com.loopers.application.product

import com.loopers.domain.Money
import com.loopers.domain.product.ProductInfo
import java.time.ZonedDateTime

data class ProductResult(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val description: String?,
    val price: Money,
    val stockQuantity: Int,
    val likeCount: Int,
    val status: String,
    val displayYn: Boolean,
    val imageUrl: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: ProductInfo, brandName: String): ProductResult {
            return ProductResult(
                id = info.id,
                brandId = info.brandId,
                brandName = brandName,
                name = info.name,
                description = info.description,
                price = info.price,
                stockQuantity = info.stockQuantity,
                likeCount = info.likeCount,
                status = info.status.name,
                displayYn = info.displayYn,
                imageUrl = info.imageUrl,
                createdAt = info.createdAt,
            )
        }
    }
}
