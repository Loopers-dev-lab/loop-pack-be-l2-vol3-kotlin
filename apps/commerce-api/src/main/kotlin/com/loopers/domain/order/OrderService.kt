package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    fun getOrder(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    fun getOrderItems(orderId: Long): List<OrderItem> {
        return orderItemRepository.findByOrderId(orderId)
    }

    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order> {
        return orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

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
