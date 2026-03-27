package com.loopers.application.handler.command.order

import com.loopers.application.order.OrderService
import com.loopers.domain.common.command.CompleteOrderCommand
import com.loopers.domain.order.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CompleteOrderCommandHandler(
    private val orderService: OrderService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CompleteOrderCommand) {
        val order = orderService.getOrderById(command.orderId)
        if (order.status == OrderStatus.PAID) {
            log.info("이미 완료된 주문 (멱등 처리): orderId={}", command.orderId)
            return
        }
        orderService.updateOrderStatus(command.orderId, OrderStatus.PAID)
    }
}
