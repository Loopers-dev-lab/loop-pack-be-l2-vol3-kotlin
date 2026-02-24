package com.loopers.application.order

data class OrderPlaceCommand(
    val productId: Long,
    val quantity: Int,
)
