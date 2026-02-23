package com.loopers.application.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component

@Component
class GetOrdersAdminUseCase(private val orderService: OrderService) {

    fun execute(page: Int, size: Int): PageResult<OrderInfo> {
        return orderService.getAllOrders(page, size)
            .map { OrderInfo.from(it) }
    }
}
