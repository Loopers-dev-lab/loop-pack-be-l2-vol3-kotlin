package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getOrderInfo(orderId: Long): OrderInfo {
        return OrderInfo.from(getOrder(orderId))
    }

    @Transactional(readOnly = true)
    fun getUserOrders(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderInfo> {
        return orderRepository.findAllByUserId(userId, startAt, endAt).map { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAllOrders(pageable: Pageable): Page<OrderInfo> {
        return orderRepository.findAll(pageable).map { OrderInfo.from(it) }
    }
}
