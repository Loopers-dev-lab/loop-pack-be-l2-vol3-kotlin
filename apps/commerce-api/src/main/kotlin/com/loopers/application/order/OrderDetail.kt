package com.loopers.application.order

import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem

data class OrderDetail(
    val order: Order,
    val items: List<OrderItem>,
)
