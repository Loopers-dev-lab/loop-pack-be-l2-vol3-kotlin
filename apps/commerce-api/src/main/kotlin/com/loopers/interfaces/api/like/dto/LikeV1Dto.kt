package com.loopers.interfaces.api.like.dto

import com.loopers.application.like.LikeInfo
import com.loopers.domain.catalog.product.entity.Product
import java.math.BigDecimal

class LikeV1Dto {
    data class LikeResponse(
        val likeId: Long,
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val productStatus: Product.ProductStatus,
    ) {
        companion object {
            fun from(info: LikeInfo): LikeResponse {
                return LikeResponse(
                    likeId = info.likeId,
                    productId = info.productId,
                    productName = info.productName,
                    productPrice = info.productPrice,
                    productStatus = info.productStatus,
                )
            }
        }
    }
}
