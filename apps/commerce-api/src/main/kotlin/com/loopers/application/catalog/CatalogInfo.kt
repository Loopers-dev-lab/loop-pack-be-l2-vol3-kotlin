package com.loopers.application.catalog

import com.loopers.application.catalog.product.ProductInfo

data class CatalogInfo(
    val product: ProductInfo,
    val brandName: String,
    val rank: Int? = null,
) {
    companion object {
        fun from(detail: ProductDetail, rank: Int? = null): CatalogInfo = CatalogInfo(
            product = ProductInfo.from(detail.product),
            brandName = detail.brand.name.value,
            rank = rank,
        )
    }
}
