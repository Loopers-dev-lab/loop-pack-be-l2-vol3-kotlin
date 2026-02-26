package com.loopers.application.catalog

import com.loopers.domain.catalog.ProductSortType

data class UserListProductsCriteria(
    val page: Int,
    val size: Int,
    val brandId: Long? = null,
    val sort: ProductSortType = ProductSortType.LATEST,
)
