package com.loopers.interfaces.api.product.dto

import com.loopers.application.catalog.product.ProductDetailInfo
import com.loopers.application.catalog.product.ProductInfo
import com.loopers.interfaces.api.brand.dto.BrandV1Dto
import java.math.BigDecimal

class ProductV1Dto {
    data class CustomerProductResponse(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val status: String,
        val likeCount: Int,
    ) {
        companion object {
            fun from(info: ProductInfo): CustomerProductResponse {
                return CustomerProductResponse(
                    id = info.id,
                    name = info.name,
                    price = info.price,
                    status = info.status,
                    likeCount = info.likeCount,
                )
            }
        }
    }

    data class ProductDetailResponse(
        val product: CustomerProductResponse,
        val brand: BrandV1Dto.BrandResponse,
    ) {
        companion object {
            fun from(info: ProductDetailInfo): ProductDetailResponse {
                return ProductDetailResponse(
                    product = CustomerProductResponse.from(info.product),
                    brand = BrandV1Dto.BrandResponse(id = info.product.brandId, name = info.brandName),
                )
            }
        }
    }
}
