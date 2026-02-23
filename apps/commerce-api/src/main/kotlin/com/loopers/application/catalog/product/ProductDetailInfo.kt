package com.loopers.application.catalog.product

import com.loopers.domain.catalog.ProductDetail

data class ProductDetailInfo(
    val product: ProductInfo,
    val brandName: String,
) {
    companion object {
        fun from(detail: ProductDetail): ProductDetailInfo = ProductDetailInfo(
            product = ProductInfo.from(detail.product),
            brandName = detail.brand.name.value,
        )
    }
}
