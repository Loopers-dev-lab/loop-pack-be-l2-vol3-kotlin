package com.loopers.application.order

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
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
    fun getOrder(orderId: Long): AdminOrderDetailInfo {
        val order = orderService.getOrderById(orderId)
        return AdminOrderDetailInfo.from(order)
    }
}
