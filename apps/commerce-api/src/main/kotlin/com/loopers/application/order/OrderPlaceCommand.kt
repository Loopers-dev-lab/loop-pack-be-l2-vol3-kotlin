package com.loopers.application.order

import com.loopers.domain.common.Quantity

data class OrderPlaceCommand(
    val productId: Long,
    val quantity: Quantity,
)
