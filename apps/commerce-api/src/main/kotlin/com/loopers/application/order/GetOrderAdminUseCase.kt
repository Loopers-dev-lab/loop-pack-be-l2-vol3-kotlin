package com.loopers.application.order

import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component

@Component
class GetOrderAdminUseCase(private val orderService: OrderService) {

    fun execute(orderId: Long): OrderInfo {
        return OrderInfo.from(orderService.getOrderForAdmin(orderId))
    }
}
