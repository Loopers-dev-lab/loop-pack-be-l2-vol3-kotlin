package com.loopers.domain.order

import com.loopers.domain.PageResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(userId: Long, products: List<OrderProductInfo>, command: OrderCommand.CreateOrder): Order {
        val order = Order.create(userId, products, command)
        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(userId: Long, orderId: Long): Order {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.refUserId != userId) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        return order
    }

    @Transactional(readOnly = true)
    fun getOrderForAdmin(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getOrdersByUserId(userId: Long, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<Order> {
        return orderRepository.findAllByUserId(userId, from, to, page, size)
    }

    @Transactional(readOnly = true)
    fun getAllOrders(page: Int, size: Int): PageResult<Order> {
        return orderRepository.findAll(page, size)
    }
}
