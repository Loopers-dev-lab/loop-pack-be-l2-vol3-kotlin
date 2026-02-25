package com.loopers.domain.order

import org.springframework.stereotype.Component

@Component
class OrderCanceller(
    private val orderReader: OrderReader,
    private val orderRepository: OrderRepository,
) {

    fun cancel(orderId: Long, memberId: Long): Order {
        val order = orderReader.getById(orderId)
        order.validateOwner(memberId)
        order.cancel()
        orderRepository.save(order)
        return order
    }
}
