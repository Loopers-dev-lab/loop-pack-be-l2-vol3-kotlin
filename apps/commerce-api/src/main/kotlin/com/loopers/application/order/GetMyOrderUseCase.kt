package com.loopers.application.order

import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, orderId: Long): OrderDetailInfo {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw CoreException(OrderErrorCode.ORDER_NOT_FOUND)

        if (order.userId != userId) {
            throw CoreException(OrderErrorCode.ORDER_ACCESS_DENIED)
        }

        val orderItems = orderItemRepository.findByOrderId(orderId)

        return OrderDetailInfo.from(order, orderItems)
    }
}
