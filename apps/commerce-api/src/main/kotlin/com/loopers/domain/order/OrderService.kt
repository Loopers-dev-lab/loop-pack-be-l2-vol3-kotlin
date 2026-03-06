package com.loopers.domain.order

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {

    fun getOrder(userId: Long, orderId: Long): Order {
        val order = getOrderById(orderId)
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "다른 사용자의 주문을 조회할 수 없습니다.")
        }
        return order
    }

    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order> {
        return orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

    fun getOrderById(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    fun getAllOrders(pageQuery: PageQuery): PageResult<Order> {
        return orderRepository.findAll(pageQuery)
    }

    fun createOrder(userId: Long, items: List<OrderItemCommand>): Order {
        val order = Order(userId = userId)
        order.addItems(items)
        return orderRepository.save(order)
    }

    fun changeStatus(orderId: Long, next: OrderStatus) {
        val order = getOrderById(orderId)
        order.changeStatus(next)
        orderRepository.saveAndFlush(order)
    }
}
