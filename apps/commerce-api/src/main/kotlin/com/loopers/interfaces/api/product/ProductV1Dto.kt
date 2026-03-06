package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo

class ProductV1Dto {
    data class ProductResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val description: String,
        val price: Long,
        val likeCount: Int,
    ) {
        companion object {
            fun from(productInfo: ProductInfo): ProductResponse {
                return ProductResponse(
                    id = productInfo.id,
                    brandId = productInfo.brand.id,
                    brandName = productInfo.brand.name,
                    name = productInfo.name,
                    description = productInfo.description,
                    price = productInfo.price,
                    likeCount = productInfo.likeCount,
                )
            }
        }
    }
}
