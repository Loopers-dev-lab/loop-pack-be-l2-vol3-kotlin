package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.CreateOrderItemCommand
import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component

@Component
class UserCreateOrderUseCase(
    private val orderService: OrderService,
) : UseCase<CreateOrderCriteria, CreateOrderResult> {

    override fun execute(criteria: CreateOrderCriteria): CreateOrderResult {
        val command = CreateOrderCommand(
            loginId = criteria.loginId,
            items = criteria.items.map {
                CreateOrderItemCommand(productId = it.productId, quantity = it.quantity)
            },
        )
        val info = orderService.createOrder(command)
        return CreateOrderResult.from(info)
    }
}
