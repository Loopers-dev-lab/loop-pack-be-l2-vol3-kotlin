package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductResult
import java.time.ZonedDateTime

class ProductV1Dto {

    data class ProductResponse(
        val id: Long,
        val brandName: String,
        val name: String,
        val description: String?,
        val price: Long,
        val likeCount: Int,
        val imageUrl: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: ProductResult): ProductResponse {
                return ProductResponse(
                    id = result.id,
                    brandName = result.brandName,
                    name = result.name,
                    description = result.description,
                    price = result.price,
                    likeCount = result.likeCount,
                    imageUrl = result.imageUrl,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
