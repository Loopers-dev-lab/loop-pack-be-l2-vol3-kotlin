package com.loopers.application.order

import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetOrderUseCase(
    private val orderRepository: OrderRepository,
) {
    fun getById(userId: Long, orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")

        order.assertOwnedBy(userId)

        return OrderInfo.from(order)
    }

    fun getByIdForAdmin(orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")
        return OrderInfo.from(order)
    }

    fun getAllByUserId(userId: Long): List<OrderInfo> {
        return orderRepository.findAllByUserId(userId).map { OrderInfo.from(it) }
    }

    fun getAll(): List<OrderInfo> {
        return orderRepository.findAll().map { OrderInfo.from(it) }
    }
}
