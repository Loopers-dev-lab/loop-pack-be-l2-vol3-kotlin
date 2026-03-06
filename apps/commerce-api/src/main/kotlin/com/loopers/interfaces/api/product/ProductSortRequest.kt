package com.loopers.interfaces.api.product

import com.loopers.domain.product.ProductSort

enum class ProductSortRequest {
    LATEST,
    PRICE_ASC,
    LIKES_DESC,
    ;

    fun toDomain(): ProductSort = when (this) {
        LATEST -> ProductSort.LATEST
        PRICE_ASC -> ProductSort.PRICE_ASC
        LIKES_DESC -> ProductSort.LIKES_DESC
    }
}
