package com.loopers.domain.order

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime
import java.util.UUID

data class OrderModel(
    val id: Long = 0,
    val memberId: Long,
    val orderNumber: String = UUID.randomUUID().toString(),
    val status: OrderStatus = OrderStatus.ORDERED,
    val orderedAt: ZonedDateTime = ZonedDateTime.now(),
    val items: List<OrderItemModel> = emptyList(),
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun addItem(orderItem: OrderItemModel): OrderModel =
        copy(items = items + orderItem)

    fun getTotalAmount(): Long = items.sumOf { it.amount }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 주문만 조회할 수 있습니다.")
        }
    }
}
