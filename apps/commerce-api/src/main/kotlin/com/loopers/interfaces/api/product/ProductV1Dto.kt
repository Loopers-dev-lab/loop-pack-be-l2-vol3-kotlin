package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo
import java.time.ZonedDateTime

class ProductV1Dto {

    data class ProductResponse(
        val id: Long,
        val brandId: Long,
        val name: String,
        val imageUrl: String,
        val description: String,
        val price: Long,
        val likeCount: Long,
        val brand: BrandResponse,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        data class BrandResponse(
            val id: Long,
            val name: String,
            val logoImageUrl: String,
        )

        companion object {
            fun from(info: ProductInfo) = ProductResponse(
                id = info.id,
                brandId = info.brandId,
                name = info.name,
                imageUrl = info.imageUrl,
                description = info.description,
                price = info.price,
                likeCount = info.likeCount,
                brand = BrandResponse(
                    id = info.brand.id,
                    name = info.brand.name,
                    logoImageUrl = info.brand.logoImageUrl,
                ),
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
            )
        }
    }
}
