package com.loopers.application.order

import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetOrderDetailUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(orderId: Long): OrderDetailInfo {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw CoreException(OrderErrorCode.ORDER_NOT_FOUND)

        val orderItems = orderItemRepository.findByOrderId(orderId)

        return OrderDetailInfo.from(order, orderItems)
    }
}
