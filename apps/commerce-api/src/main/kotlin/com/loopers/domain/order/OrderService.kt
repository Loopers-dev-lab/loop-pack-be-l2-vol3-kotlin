package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {

    fun getOrder(userId: Long, orderId: Long): Order {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "다른 사용자의 주문을 조회할 수 없습니다.")
        }
        return order
    }

    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order> {
        if (startAt.isAfter(endAt)) {
            throw CoreException(ErrorType.BAD_REQUEST, "시작일이 종료일보다 클 수 없습니다.")
        }
        return orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

    fun createOrder(userId: Long, items: List<OrderItemCommand>): Order {
        validateOrderItems(items)
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

    private fun validateOrderItems(items: List<OrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }
        if (items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.")
        }
        val productIds = items.map { it.productId }
        if (productIds.size != productIds.toSet().size) {
            throw CoreException(ErrorType.BAD_REQUEST, "중복된 상품이 포함되어 있습니다.")
        }
    }
}
