package com.loopers.application.order

import com.loopers.domain.order.OrderService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminOrderFacade(
    private val orderService: OrderService,
) {
    @Transactional(readOnly = true)
    fun getOrders(page: Int, size: Int): Page<OrderInfo> {
        return orderService.getOrders(page, size).map { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        return orderService.getOrderById(orderId)
            .let { OrderInfo.from(it) }
    }
}
