package com.loopers.application.handler.command.order

import com.loopers.application.order.OrderService
import com.loopers.domain.common.command.CompleteOrderCommand
import com.loopers.domain.order.OrderStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CompleteOrderCommandHandler(
    private val orderService: OrderService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CompleteOrderCommand) {
        orderService.updateOrderStatus(command.orderId, OrderStatus.PAID)
    }
}
