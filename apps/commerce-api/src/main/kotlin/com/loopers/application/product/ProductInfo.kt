package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.SaleStatus
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val name: String,
    val price: Long,
    val brandId: Long,
    val brandName: String,
    val description: String?,
    val thumbnailImageUrl: String?,
    val stockQuantity: Int,
    val likesCount: Long,
    val saleStatus: SaleStatus,
    val displayStatus: DisplayStatus,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun of(product: ProductModel, brand: BrandModel): ProductInfo {
            return ProductInfo(
                id = product.id,
                name = product.name,
                price = product.price,
                brandId = product.brandId,
                brandName = brand.name,
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
}
