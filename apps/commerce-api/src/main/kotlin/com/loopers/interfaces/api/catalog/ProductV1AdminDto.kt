package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.GetProductResult
import com.loopers.application.catalog.ListProductsResult
import java.math.BigDecimal

class ProductV1AdminDto {
    data class RegisterRequest(
        val brandId: Long,
        val name: String,
        val quantity: Int,
        val price: BigDecimal,
    )

    data class ProductResponse(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val brandName: String,
    ) {
        companion object {
            fun from(result: GetProductResult): ProductResponse {
                return ProductResponse(
                    id = result.id,
                    name = result.name,
                    price = result.price,
                    brandName = result.brandName,
                )
            }
        }
    }

    data class ProductDetailResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val quantity: Int,
        val price: BigDecimal,
    ) {
        companion object {
            fun from(result: GetProductResult): ProductDetailResponse {
                return ProductDetailResponse(
                    id = result.id,
                    brandId = result.brandId,
                    brandName = result.brandName,
                    name = result.name,
                    quantity = result.quantity,
                    price = result.price,
                )
            }
        }
    }

    data class ProductSliceResponse(
        val content: List<ProductResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: ListProductsResult): ProductSliceResponse {
                return ProductSliceResponse(
                    content = result.content.map { ProductResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }

    data class UpdateRequest(
        val newName: String,
        val newQuantity: Int,
        val newPrice: BigDecimal,
    )
}
