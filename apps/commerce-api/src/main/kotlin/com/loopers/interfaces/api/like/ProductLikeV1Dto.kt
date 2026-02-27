package com.loopers.interfaces.api.like

import com.loopers.application.catalog.UserGetProductResult
import com.loopers.application.like.LikedProductsResult
import java.math.BigDecimal

class ProductLikeV1Dto {
    data class LikedProductResponse(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val brandName: String,
    ) {
        companion object {
            fun from(result: UserGetProductResult): LikedProductResponse {
                return LikedProductResponse(
                    id = result.id,
                    name = result.name,
                    price = result.price,
                    brandName = result.brandName,
                )
            }
        }
    }

    data class LikedProductSliceResponse(
        val content: List<LikedProductResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: LikedProductsResult): LikedProductSliceResponse {
                return LikedProductSliceResponse(
                    content = result.content.map { LikedProductResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }
}
