package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val brandName: String,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
) {
    companion object {
        fun from(info: ProductInfo): ProductResponse {
            return ProductResponse(
                id = info.id,
                name = info.name,
                price = info.price,
                brandName = info.brandName,
                imageUrl = info.imageUrl,
                likeCount = info.likeCount,
                available = info.available,
            )
        }
    }
}
