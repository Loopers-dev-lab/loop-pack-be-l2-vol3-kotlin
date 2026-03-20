package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.SaleStatus
import java.time.ZonedDateTime

data class ProductCacheSnapshot(
    val id: Long,
    val name: String,
    val price: Long,
    val brandId: Long,
    val description: String?,
    val thumbnailImageUrl: String?,
    val stockQuantity: Int,
    val likesCount: Long,
    val saleStatus: SaleStatus,
    val displayStatus: DisplayStatus,
    val createdAt: ZonedDateTime?,
) {
    fun toProductInfo(brand: BrandModel): ProductInfo = ProductInfo(
            id = id,
            name = name,
            price = price,
            brandId = brandId,
            brandName = brand.name,
            description = description,
            thumbnailImageUrl = thumbnailImageUrl,
            stockQuantity = stockQuantity,
            likesCount = likesCount,
            saleStatus = saleStatus,
            displayStatus = displayStatus,
            createdAt = createdAt,
        )

    companion object {
        fun from(product: ProductModel): ProductCacheSnapshot = ProductCacheSnapshot(
                id = product.id,
                name = product.name,
                price = product.price,
                brandId = product.brandId,
                description = product.description,
                thumbnailImageUrl = product.thumbnailImageUrl,
                stockQuantity = product.stockQuantity,
                likesCount = product.likesCount,
                saleStatus = product.saleStatus,
                displayStatus = product.displayStatus,
                createdAt = runCatching { product.createdAt }.getOrNull(),
            )
    }
}
