package com.loopers.application.order

import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.refUserId != userId) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        val items = orderItemRepository.findAllByOrderId(orderId)
        return OrderInfo.from(OrderDetail(order, items))
    }
}
