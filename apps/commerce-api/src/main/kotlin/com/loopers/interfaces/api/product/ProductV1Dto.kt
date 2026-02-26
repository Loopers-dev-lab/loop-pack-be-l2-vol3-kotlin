package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductListResult

class ProductV1Dto {
    data class ProductResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String?,
        val name: String,
        val description: String,
        val price: Long,
        val imageUrl: String,
        val likeCount: Int,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(info: ProductInfo): ProductResponse {
                return ProductResponse(
                    id = info.id,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    imageUrl = info.imageUrl,
                    likeCount = info.likeCount,
                    soldOut = info.soldOut,
                )
            }
        }
    }

    data class ProductListResponse(
        val data: List<ProductResponse>,
        val nextCursor: String?,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: ProductListResult): ProductListResponse {
                return ProductListResponse(
                    data = result.data.map { ProductResponse.from(it) },
                    nextCursor = result.nextCursor,
                    hasNext = result.hasNext,
                )
            }
        }
    }
}
