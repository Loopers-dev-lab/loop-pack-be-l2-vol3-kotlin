package com.loopers.domain.order

import com.loopers.domain.product.Money
import java.time.ZonedDateTime

class Order private constructor(
    val persistenceId: Long?,
    val refUserId: Long,
    val status: OrderStatus,
    val totalAmount: Money,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItem>,
) {

    fun cancel(): Order {
        if (status != OrderStatus.PENDING) {
            throw OrderException(
                OrderError.NOT_CANCELLABLE,
                "PENDING 상태에서만 취소할 수 있습니다. 현재 상태: $status",
            )
        }
        return Order(
            persistenceId = persistenceId,
            refUserId = refUserId,
            status = OrderStatus.CANCELLED,
            totalAmount = totalAmount,
            orderedAt = orderedAt,
            items = items,
        )
    }

    fun complete(): Order {
        if (status != OrderStatus.PENDING) {
            throw OrderException(
                OrderError.NOT_COMPLETABLE,
                "PENDING 상태에서만 완료할 수 있습니다. 현재 상태: $status",
            )
        }
        return Order(
            persistenceId = persistenceId,
            refUserId = refUserId,
            status = OrderStatus.COMPLETED,
            totalAmount = totalAmount,
            orderedAt = orderedAt,
            items = items,
        )
    }

    fun assertOwnedBy(userId: Long) {
        if (this.refUserId != userId) {
            throw OrderException(OrderError.NOT_OWNED, "타인의 주문입니다.")
        }
    }

    fun isOwnedBy(userId: Long): Boolean {
        return this.refUserId == userId
    }

    fun canCancel(): Boolean {
        return status == OrderStatus.PENDING
    }

    companion object {
        fun create(userId: Long, items: List<OrderItem>): Order {
            require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다." }
            val totalAmount = items.fold(Money(0)) { acc, item ->
                acc.add(item.getSubtotal())
            }
            return Order(
                persistenceId = null,
                refUserId = userId,
                status = OrderStatus.PENDING,
                totalAmount = totalAmount,
                orderedAt = ZonedDateTime.now(),
                items = items,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            refUserId: Long,
            status: OrderStatus,
            totalAmount: Money,
            orderedAt: ZonedDateTime,
            items: List<OrderItem>,
        ): Order {
            return Order(
                persistenceId = persistenceId,
                refUserId = refUserId,
                status = status,
                totalAmount = totalAmount,
                orderedAt = orderedAt,
                items = items,
            )
        }
    }
}
