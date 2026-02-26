package com.loopers.interfaces.apiadmin.product

import com.loopers.application.product.ProductInfo

class AdminProductDto {
    data class CreateRequest(
        val name: String,
        val description: String?,
        val price: Long,
        val stockQuantity: Int,
        val brandId: Long,
    )

    data class CreateResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val price: Long,
        val stockQuantity: Int,
        val brandId: Long,
    ) {
        companion object {
            fun from(info: ProductInfo): CreateResponse {
                return CreateResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    stockQuantity = info.stockQuantity,
                    brandId = info.brandId,
                )
            }
        }
    }
}
