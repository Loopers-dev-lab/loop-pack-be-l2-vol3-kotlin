package com.loopers.domain.order.model

import com.loopers.domain.common.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.order.OrderProductInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.time.ZonedDateTime

class Order private constructor(
    val refUserId: Long,
    status: OrderStatus,
    totalPrice: Money,
    val items: List<OrderItem> = emptyList(),
    val deletedAt: ZonedDateTime? = null,
) {

    val id: Long = 0

    var status: OrderStatus = status
        private set

    var totalPrice: Money = totalPrice
        private set

    enum class OrderStatus {
        CREATED,
        PAID,
        CANCELLED,
        FAILED,
    }

    @OptIn(AggregateRootOnly::class)
    fun cancelItem(item: OrderItem) {
        if (item.status == OrderItem.ItemStatus.CANCELLED) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문 아이템입니다.")
        }
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
