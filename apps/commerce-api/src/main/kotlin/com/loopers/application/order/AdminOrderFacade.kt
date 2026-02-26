package com.loopers.application.order

import com.loopers.domain.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminOrderFacade(
    private val orderService: OrderService,
) {
    @Transactional(readOnly = true)
    fun getOrders(page: Int, size: Int): PageResult<OrderInfo> {
        val result = orderService.getOrders(page, size)
        return PageResult(
            content = result.content.map { OrderInfo.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        return orderService.getOrderById(orderId)
            .let { OrderInfo.from(it) }
    }
}
