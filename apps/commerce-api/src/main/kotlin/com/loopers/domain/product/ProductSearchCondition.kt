package com.loopers.domain.product

data class ProductSearchCondition(
    val brandId: Long? = null,
    val sort: ProductSortType = ProductSortType.LATEST,
    val page: Int = 0,
    val size: Int = 20,
    val includeDeleted: Boolean = false,
)
