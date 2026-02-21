package com.loopers.domain.order

import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.entity.OrderItem

data class OrderDetail(
    val order: Order,
    val items: List<OrderItem>,
)
