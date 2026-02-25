package com.loopers.domain.order.model

import com.loopers.domain.common.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.order.OrderProductInfo
import java.math.BigDecimal
import java.time.ZonedDateTime

class Order private constructor(
    val id: Long = 0,
    val refUserId: Long,
    status: OrderStatus,
    totalPrice: Money,
    val items: List<OrderItem> = emptyList(),
    val deletedAt: ZonedDateTime? = null,
) {
    var status: OrderStatus = status
        private set

    var totalPrice: Money = totalPrice
        private set

    fun isDeleted(): Boolean = deletedAt != null

    enum class OrderStatus {
        CREATED,
        PAID,
        CANCELLED,
        FAILED,
    }

    @OptIn(AggregateRootOnly::class)
    fun cancelItem(item: OrderItem) {
        item.cancel()
        totalPrice -= (item.productPrice * item.quantity)
    }

    @OptIn(AggregateRootOnly::class)
    fun assignOrderIdToItems(orderId: Long) {
        items.forEach { it.assignToOrder(orderId) }
    }

    companion object {
        fun create(userId: Long, items: List<Pair<OrderProductInfo, Int>>): Order {
            val orderItems = items.map { (info, quantity) ->
                OrderItem.create(info, quantity)
            }
            val totalPrice = orderItems.fold(Money(BigDecimal.ZERO)) { acc, item ->
                acc + (item.productPrice * item.quantity)
            }
            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                totalPrice = totalPrice,
                items = orderItems,
            )
        }

        fun fromPersistence(
            id: Long,
            refUserId: Long,
            status: OrderStatus,
            totalPrice: Money,
            deletedAt: ZonedDateTime?,
            items: List<OrderItem> = emptyList(),
        ): Order {
            return Order(
                refUserId = refUserId,
                status = status,
                totalPrice = totalPrice,
                items = items,
                deletedAt = deletedAt,
            ).also {
                Order::class.java.getDeclaredField("id").apply {
                    isAccessible = true
                    set(it, id)
                }
            }
        }
    }
}
