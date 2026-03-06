package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.product.Product

data class ProductInfo(
    val id: Long,
    val brand: BrandInfo,
    val name: String,
    val description: String,
    val price: Long,
    val stockQuantity: Int,
    val likeCount: Int,
) {
    companion object {
        fun from(product: Product, brandInfo: BrandInfo): ProductInfo {
            return ProductInfo(
                id = product.id,
                brand = brandInfo,
                name = product.name,
                description = product.description,
                price = product.price,
                stockQuantity = product.stockQuantity,
                likeCount = product.likeCount,
            )
        }
    }
}
