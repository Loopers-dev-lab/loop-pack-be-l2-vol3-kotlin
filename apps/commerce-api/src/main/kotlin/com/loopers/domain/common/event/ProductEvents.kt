package com.loopers.domain.common.event

data class ProductCreatedEvent(
    val productId: Long,
    val brandId: Long,
)

data class ProductUpdatedEvent(
    val productId: Long,
)

data class ProductDeletedEvent(
    val productId: Long,
)

data class StockDeductedEvent(
    val productId: Long,
    val quantity: Int,
)

data class StockRestoredEvent(
    val productId: Long,
    val quantity: Int,
)
