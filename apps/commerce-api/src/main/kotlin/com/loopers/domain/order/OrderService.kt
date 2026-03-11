package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(userId: Long, items: List<OrderItem>): Order {
        val order = Order.create(userId = userId, items = items)
        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Order =
        orderRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "[$id] 해당 ID에 해당하는 주문이 존재하지 않습니다.")

    @Transactional(readOnly = true)
    fun getOrders(userId: Long, startAt: LocalDate?, endAt: LocalDate?): List<Order> = when {
        startAt == null && endAt == null -> orderRepository.findByUserId(userId)
        startAt != null && endAt != null -> orderRepository.findByUserIdAndDateRange(userId, startAt, endAt)
        else -> throw CoreException(ErrorType.BAD_REQUEST, "startAt과 endAt은 모두 제공되거나 모두 생략되어야 합니다.")
    }

    @Transactional(readOnly = true)
    fun findAll(page: Int, size: Int): List<Order> = orderRepository.findAll(page, size)
}
