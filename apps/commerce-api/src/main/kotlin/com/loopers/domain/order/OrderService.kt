package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
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
    fun getOrder(id: Long): Order {
        return orderRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun getUserOrders(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<Order> {
        return orderRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

    @Transactional(readOnly = true)
    fun getOrders(pageable: Pageable): Page<Order> {
        return orderRepository.findAllByDeletedAtIsNull(pageable)
    }
}
