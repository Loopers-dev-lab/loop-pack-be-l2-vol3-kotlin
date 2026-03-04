package com.loopers.domain.product

import com.loopers.domain.common.Quantity

data class StockDeductionRequest(
    val productId: Long,
    val quantity: Quantity,
)
