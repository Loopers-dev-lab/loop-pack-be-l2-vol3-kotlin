package com.loopers.application.order

import com.loopers.domain.order.OrderReader
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val orderReader: OrderReader,
) {

    @Transactional
    fun createOrder(memberId: Long, command: CreateOrderCommand): OrderInfo.Detail {
        val items = command.items.map {
            OrderService.OrderItemCommand(
                productId = it.productId,
                quantity = it.quantity,
            )
        }
        val order = orderService.createOrder(memberId, items)
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getById(orderId: Long, memberId: Long): OrderInfo.Detail {
        val order = orderReader.getById(orderId)
        order.validateOwner(memberId)
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(memberId: Long): List<OrderInfo.Main> {
        return orderReader.getAllByMemberId(memberId).map { OrderInfo.Main.from(it) }
    }

    @Transactional
    fun cancel(orderId: Long, memberId: Long) {
        orderService.cancelOrder(orderId, memberId)
    }

    data class CreateOrderCommand(val items: List<OrderItemRequest>)
    data class OrderItemRequest(val productId: Long, val quantity: Int)
}
