package com.loopers.domain.order

import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(userId: Long, items: List<CreateOrderItemCommand>): Long {
        validateItems(items)

        val order = Order.create(userId)
        val savedOrder = orderRepository.save(order)

        items.forEach { itemRequest ->
            val orderItem = OrderItem.create(
                orderId = savedOrder.id,
                productId = itemRequest.productId,
                quantity = itemRequest.quantity,
                price = itemRequest.price,
                productName = itemRequest.productName,
            )
            savedOrder.addOrderItem(orderItem)
        }

        return savedOrder.id
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> {
        return orderRepository.findByUserId(userId, pageable).map { OrderedInfo.from(it) }
    }

    fun getOrderById(userId: Long, orderId: Long): Order =
        orderRepository.findById(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForAdmin(orderId: Long): Order =
        orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    private fun validateItems(items: List<CreateOrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다")
        }

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다")
            }
        }
    }
}
