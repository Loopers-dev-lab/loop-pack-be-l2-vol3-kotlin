package com.loopers.interfaces.api.user.product

import com.loopers.application.user.product.UserProductResult
import java.math.BigDecimal

class UserProductV1Response {
    data class Detail(
        val id: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val likeCount: Int,
        val stockQuantity: Int,
    ) {
        companion object {
            fun from(result: UserProductResult.Detail): Detail = Detail(
                id = result.id,
                name = result.name,
                regularPrice = result.regularPrice,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                imageUrl = result.imageUrl,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
                stockQuantity = result.stockQuantity,
            )
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val thumbnailUrl: String?,
        val likeCount: Int,
    ) {
        companion object {
            fun from(result: UserProductResult.Summary): Summary = Summary(
                id = result.id,
                name = result.name,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
            )
        }
    }
}
