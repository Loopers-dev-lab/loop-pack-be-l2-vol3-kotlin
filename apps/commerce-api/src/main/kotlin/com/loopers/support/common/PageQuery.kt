package com.loopers.support.common

data class PageQuery(
    val page: Int,
    val size: Int,
    val sort: SortOrder = SortOrder.UNSORTED,
)
