package com.loopers.domain.order

import org.springframework.stereotype.Component

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    fun createOrder(userId: Long, items: List<OrderItemCommand>): Order {
        val totalAmount = items.sumOf { it.productPrice * it.quantity }
        val order = orderRepository.save(
            Order(userId = userId, totalAmount = totalAmount),
        )
        val orderItems = items.map { item ->
            OrderItem(
                orderId = order.id,
                productId = item.productId,
                quantity = item.quantity,
                productName = item.productName,
                productPrice = item.productPrice,
                brandName = item.brandName,
            )
        }
        orderItemRepository.saveAll(orderItems)
        return order
    }
}
