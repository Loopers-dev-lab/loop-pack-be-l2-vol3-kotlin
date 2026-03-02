package com.loopers.domain.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.order.TotalAmount
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun createOrder(userId: Long, totalAmount: Long): OrderModel {
        return orderRepository.save(
            OrderModel(userId = userId, totalAmount = TotalAmount(totalAmount), status = OrderStatus.PENDING_PAYMENT)
        )
    }

    @Transactional(readOnly = true)
    fun getOrders(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime, pageable: Pageable): Page<OrderModel> =
        orderRepository.findAllByUserId(userId, startAt, endAt, pageable)

    @Transactional(readOnly = true)
    fun getOrderById(id: Long): OrderModel =
        orderRepository.findById(id) ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

    @Transactional(readOnly = true)
    fun getAllOrders(pageable: Pageable): Page<OrderModel> =
        orderRepository.findAll(pageable)
}
