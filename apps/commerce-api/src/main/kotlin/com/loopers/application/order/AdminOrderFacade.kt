package com.loopers.application.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminOrderFacade(
    private val orderService: OrderService,
) {

    @Transactional(readOnly = true)
    fun getOrders(pageQuery: PageQuery): PageResult<AdminOrderInfo> {
        return orderService.getAllOrders(pageQuery)
            .map { AdminOrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderDetailInfo {
        val order = orderService.getOrderById(orderId)
        return OrderDetailInfo.from(order)
    }
}
