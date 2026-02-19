package com.loopers.interfaces.api.product

import com.loopers.domain.catalog.ProductDetail
import com.loopers.domain.catalog.product.Product
import com.loopers.interfaces.api.brand.BrandV1Dto
import java.math.BigDecimal

class ProductV1Dto {
    data class CustomerProductResponse(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val status: Product.ProductStatus,
        val likeCount: Int,
    ) {
        companion object {
            fun from(product: Product): CustomerProductResponse {
                return CustomerProductResponse(
                    id = product.id,
                    name = product.name,
                    price = product.price,
                    status = product.status,
                    likeCount = product.likeCount,
                )
            }
        }
    }

    data class ProductDetailResponse(
        val product: CustomerProductResponse,
        val brand: BrandV1Dto.BrandResponse,
    ) {
        companion object {
            fun from(detail: ProductDetail): ProductDetailResponse {
                return ProductDetailResponse(
                    product = CustomerProductResponse.from(detail.product),
                    brand = BrandV1Dto.BrandResponse.from(detail.brand),
                )
            }
        }
    }
}
