package com.loopers.domain.event

data class OrderLineItem(
    val productId: Long,
    val quantity: Int,
)
