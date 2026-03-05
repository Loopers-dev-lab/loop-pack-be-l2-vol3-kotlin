package com.loopers.domain.product

data class ProductSearchCondition(
    val brandId: Long? = null,
    val sort: ProductSort = ProductSort.LATEST,
    val size: Int = 20,
    val cursor: String? = null,
)

enum class ProductSort {
    LATEST,
    PRICE_ASC,
    LIKES_DESC,
}
