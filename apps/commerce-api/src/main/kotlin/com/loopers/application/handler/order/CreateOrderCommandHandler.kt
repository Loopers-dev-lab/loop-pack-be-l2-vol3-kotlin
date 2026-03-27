package com.loopers.application.handler.order

import com.loopers.application.order.OrderCommand
import com.loopers.application.order.OrderService
import com.loopers.domain.common.command.CreateOrderCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CreateOrderCommandHandler(
    private val orderService: OrderService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CreateOrderCommand) {
        orderService.createOrder(
            memberId = command.memberId,
            items = command.items.map {
                OrderCommand.CreateOrderItem(
                    productId = it.productId,
                    quantity = it.quantity,
                    productName = it.productName,
                    productPrice = it.productPrice,
                    brandName = it.brandName,
                )
            },
            couponId = command.couponId,
            discountAmount = command.discountAmount,
        )
    }
}
