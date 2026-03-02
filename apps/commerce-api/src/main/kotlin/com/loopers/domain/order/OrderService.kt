package com.loopers.domain.order

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
    private val orderDomainService: OrderDomainService,
) {

    @Transactional
    fun createOrder(command: CreateOrderCommand): Order {
        val order = orderDomainService.placeOrder(command)
        val savedOrder = orderRepository.save(order)
        savedOrder.generateOrderNumber()
        return savedOrder
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Order {
        return orderRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.")
    }

    @Transactional(readOnly = true)
    fun findByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Order> {
        return orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Order> {
        return orderRepository.findAll(pageable)
    }
}
