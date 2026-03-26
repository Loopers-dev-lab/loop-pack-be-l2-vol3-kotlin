package com.loopers.domain.order

interface OrderStatusQueryRepository {
    fun findStatusById(orderId: Long): Order.Status?
}
