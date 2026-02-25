package com.loopers.domain.order

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRegister(
    private val orderRepository: OrderRepository,
) {

    fun register(memberId: Long, orderItemCommands: List<OrderItemCommand>): Order {
        val orderItems = orderItemCommands.map { command ->
            OrderItem(
                productId = command.productId,
                productName = command.productName,
                productPrice = command.productPrice,
                quantity = command.quantity,
            )
        }

        val totalPrice = orderItems.sumOf { it.subtotal }

        val order = Order(
            memberId = memberId,
            orderItems = orderItems,
            totalPrice = totalPrice,
            orderedAt = ZonedDateTime.now(),
        )

        return orderRepository.save(order)
    }

    data class OrderItemCommand(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val quantity: Int,
    )
}
