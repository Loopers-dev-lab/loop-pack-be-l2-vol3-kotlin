package com.loopers.domain.order.model

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.common.vo.Quantity
import java.math.BigDecimal
import java.time.ZonedDateTime

class Order private constructor(
    val id: OrderId = OrderId(0),
    val refUserId: UserId,
    status: OrderStatus,
    val originalPrice: Money,
    val discountAmount: Money,
    totalPrice: Money,
    val refCouponId: Long? = null,
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
        totalPrice -= (item.productPrice * item.quantity.value)
    }

    @OptIn(AggregateRootOnly::class)
    fun assignOrderIdToItems(orderId: OrderId) {
        items.forEach { it.assignToOrder(orderId) }
    }

    companion object {
        fun create(
            userId: UserId,
            items: List<Pair<OrderProductData, Quantity>>,
            discountAmount: Money = Money(BigDecimal.ZERO),
            refCouponId: Long? = null,
        ): Order {
            require(items.isNotEmpty()) { "주문은 최소 하나 이상의 항목을 포함해야 합니다." }
            val orderItems = items.map { (info, quantity) ->
                OrderItem.create(info, quantity)
            }
            val originalPrice = orderItems.fold(Money(BigDecimal.ZERO)) { acc, item ->
                acc + (item.productPrice * item.quantity.value)
            }
            val totalPrice = originalPrice - discountAmount
            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                originalPrice = originalPrice,
                discountAmount = discountAmount,
                totalPrice = totalPrice,
                refCouponId = refCouponId,
                items = orderItems,
            )
        }

        fun fromPersistence(
            id: OrderId,
            refUserId: UserId,
            status: OrderStatus,
            originalPrice: Money,
            discountAmount: Money,
            totalPrice: Money,
            refCouponId: Long?,
            deletedAt: ZonedDateTime?,
            items: List<OrderItem> = emptyList(),
        ): Order {
            return Order(
                id = id,
                refUserId = refUserId,
                status = status,
                originalPrice = originalPrice,
                discountAmount = discountAmount,
                totalPrice = totalPrice,
                refCouponId = refCouponId,
                items = items,
                deletedAt = deletedAt,
            )
        }
    }
}
