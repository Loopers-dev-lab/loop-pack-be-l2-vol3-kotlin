package com.loopers.domain.order

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRegister(
    private val orderRepository: OrderRepository,
) {

    fun register(memberId: Long, orderItems: List<OrderItem>): Order {
        val totalPrice = orderItems.sumOf { it.subtotal }

        val order = Order(
            memberId = memberId,
            orderItems = orderItems,
            totalPrice = totalPrice,
            orderedAt = ZonedDateTime.now(),
        )

        return orderRepository.save(order)
    }
}
