package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {

    fun getOrder(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order> {
        return orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

    fun createOrder(userId: Long, items: List<OrderItemCommand>): Order {
        val order = Order(userId = userId)
        items.forEach { item ->
            order.addItem(
                productId = item.productId,
                quantity = item.quantity,
                productName = item.productName,
                productPrice = item.productPrice,
                brandName = item.brandName,
            )
        }
        return orderRepository.save(order)
    }
}
