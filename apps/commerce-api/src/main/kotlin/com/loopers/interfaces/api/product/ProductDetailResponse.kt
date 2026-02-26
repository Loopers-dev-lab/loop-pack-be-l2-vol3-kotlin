package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo

data class ProductDetailResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: Long,
    val brandName: String,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
) {
    companion object {
        fun from(info: ProductInfo): ProductDetailResponse {
            return ProductDetailResponse(
                id = info.id,
                name = info.name,
                description = info.description,
                price = info.price,
                brandName = info.brandName,
                imageUrl = info.imageUrl,
                likeCount = info.likeCount,
                available = info.available,
            )
        }
    }
}
