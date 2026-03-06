package com.loopers.application.order

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.OptimisticLockingFailureException
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

    fun changeOrderStatus(orderId: Long, next: OrderStatus) {
        try {
            orderService.changeStatus(orderId, next)
        } catch (e: OptimisticLockingFailureException) {
            throw CoreException(ErrorType.CONFLICT, "주문 상태가 이미 변경되었습니다. 다시 시도해주세요.")
        }
    }
}
