package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Order(
    val id: Long? = null,
    val memberId: Long,
    val orderItems: List<OrderItem>,
    val totalPrice: Long,
    val discountAmount: Long = 0L,
    val finalPrice: Long,
    val couponId: Long? = null,
    val orderedAt: ZonedDateTime,
    status: OrderStatus = OrderStatus.ORDERED,
) {
    var status: OrderStatus = status
        private set

    init {
        if (orderItems.isEmpty()) {
            throw CoreException(ErrorType.ORDER_ITEM_EMPTY)
        }
        require(finalPrice == totalPrice - discountAmount) {
            "최종 결제액이 올바르지 않습니다. finalPrice=$finalPrice, totalPrice=$totalPrice, discountAmount=$discountAmount"
        }
    }

    fun cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw CoreException(ErrorType.ORDER_ALREADY_CANCELLED)
        }
        this.status = OrderStatus.CANCELLED
    }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.ORDER_NOT_OWNER)
        }
    }
}
