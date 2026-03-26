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
    val couponId: Long? = null,
    val discountAmount: Long = 0,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun addItem(orderItem: OrderItemModel): OrderModel =
        copy(items = items + orderItem)

    fun getOriginalAmount(): Long = items.sumOf { it.amount }

    fun getTotalAmount(): Long = getOriginalAmount() - discountAmount

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 주문만 조회할 수 있습니다.")
        }
    }

    fun requestPayment(): OrderModel {
        if (status != OrderStatus.ORDERED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 요청은 ORDERED 상태에서만 가능합니다.")
        }
        return copy(status = OrderStatus.PAYMENT_PENDING)
    }

    fun completePayment(): OrderModel {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 완료는 PAYMENT_PENDING 상태에서만 가능합니다.")
        }
        return copy(status = OrderStatus.PAID)
    }

    fun cancelByPaymentFailure(): OrderModel {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 실패 취소는 PAYMENT_PENDING 상태에서만 가능합니다.")
        }
        return copy(status = OrderStatus.CANCELLED)
    }

    fun revertToOrdered(): OrderModel {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PAYMENT_PENDING 상태에서만 ORDERED로 되돌릴 수 있습니다.")
        }
        return copy(status = OrderStatus.ORDERED)
    }
}
