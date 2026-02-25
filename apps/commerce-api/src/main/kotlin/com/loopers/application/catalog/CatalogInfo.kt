package com.loopers.application.catalog

import com.loopers.application.catalog.product.ProductInfo
import com.loopers.domain.catalog.ProductDetail

data class CatalogInfo(
    val product: ProductInfo,
    val brandName: String,
) {
    companion object {
        fun from(detail: ProductDetail): CatalogInfo = CatalogInfo(
            product = ProductInfo.from(detail.product),
            brandName = detail.brand.name.value,
        )
    }
}
