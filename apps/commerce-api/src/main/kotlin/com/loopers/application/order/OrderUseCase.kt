package com.loopers.application.order

import com.loopers.domain.order.OrderCanceller
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRegister
import com.loopers.domain.product.ProductStockDeductor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderUseCase(
    private val orderRegister: OrderRegister,
    private val orderReader: OrderReader,
    private val orderCanceller: OrderCanceller,
    private val productStockDeductor: ProductStockDeductor,
) {

    @Transactional
    fun createOrder(memberId: Long, command: CreateOrderCommand): OrderInfo.Detail {
        val orderItems = command.items.map { item ->
            val product = productStockDeductor.deductStock(item.productId, item.quantity)
            OrderItem.from(product, item.quantity)
        }
        val order = orderRegister.register(memberId, orderItems)
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
        val order = orderCanceller.cancel(orderId, memberId)
        order.orderItems.forEach { item ->
            productStockDeductor.restoreStock(item.productId, item.quantity)
        }
    }

    data class CreateOrderCommand(val items: List<OrderItemRequest>)
    data class OrderItemRequest(val productId: Long, val quantity: Int)
}
