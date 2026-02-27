package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductInfo

class AdminProductV1Dto {
    data class CreateProductRequest(
        val brandId: Long,
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
    )

    data class UpdateProductRequest(
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
    )

    data class ProductResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
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
                    stockQuantity = productInfo.stockQuantity,
                    likeCount = productInfo.likeCount,
                )
            }
        }
    }
}
