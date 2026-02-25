package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderCanceller
import com.loopers.domain.order.OrderRegister
import com.loopers.domain.product.ProductStockDeductor
import org.springframework.stereotype.Component

@Component
class OrderService(
    private val orderRegister: OrderRegister,
    private val orderCanceller: OrderCanceller,
    private val productStockDeductor: ProductStockDeductor,
) {

    fun createOrder(memberId: Long, items: List<OrderItemCommand>): Order {
        val orderItemCommands = items.map { item ->
            val product = productStockDeductor.deductStock(item.productId, item.quantity)
            OrderRegister.OrderItemCommand(
                productId = product.id!!,
                productName = product.name.value,
                productPrice = product.price.value,
                quantity = item.quantity,
            )
        }
        return orderRegister.register(memberId, orderItemCommands)
    }

    fun cancelOrder(orderId: Long, memberId: Long) {
        val order = orderCanceller.cancel(orderId, memberId)
        order.orderItems.forEach { item ->
            productStockDeductor.restoreStock(item.productId, item.quantity)
        }
    }

    data class OrderItemCommand(
        val productId: Long,
        val quantity: Int,
    )
}
