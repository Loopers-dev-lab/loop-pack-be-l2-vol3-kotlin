package com.loopers.infrastructure.cache

import com.loopers.application.brand.BrandInfo
import com.loopers.application.product.ProductInfo

data class ProductCacheDto(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val brandDescription: String,
    val name: String,
    val description: String,
    val price: Long,
    val stockQuantity: Int,
    val likeCount: Int,
) {
    fun toProductInfo(): ProductInfo {
        return ProductInfo(
            id = id,
            brand = BrandInfo(
                id = brandId,
                name = brandName,
                description = brandDescription,
            ),
            name = name,
            description = description,
            price = price,
            stockQuantity = stockQuantity,
            likeCount = likeCount,
        )
    }

    companion object {
        fun from(productInfo: ProductInfo): ProductCacheDto {
            return ProductCacheDto(
                id = productInfo.id,
                brandId = productInfo.brand.id,
                brandName = productInfo.brand.name,
                brandDescription = productInfo.brand.description,
                name = productInfo.name,
                description = productInfo.description,
                price = productInfo.price,
                stockQuantity = productInfo.stockQuantity,
                likeCount = productInfo.likeCount,
            )
        }
    }
}
