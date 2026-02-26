package com.loopers.domain.product

data class StockDeductionRequest(
    val productId: Long,
    val quantity: Int,
)
