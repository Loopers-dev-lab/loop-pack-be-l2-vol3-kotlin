package com.loopers.domain.order

data class OrderResult(
    val order: Order,
    val excludedItems: List<ExcludedItem>,
)

data class ExcludedItem(
    val productId: Long,
    val reason: String,
)
