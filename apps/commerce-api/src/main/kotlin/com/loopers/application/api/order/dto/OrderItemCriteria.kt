package com.loopers.application.api.order.dto

data class OrderItemCriteria(
    val productId: Long,
    val quantity: Int,
)
