package com.loopers.application.order

import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component

@Component
class GetOrderUseCase(private val orderService: OrderService) {

    fun execute(userId: Long, orderId: Long): OrderInfo {
        return OrderInfo.from(orderService.getOrder(userId, orderId))
    }
}
