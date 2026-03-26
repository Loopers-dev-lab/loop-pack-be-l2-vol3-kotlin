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

    fun beginPayment() {
        if (status == OrderStatus.CANCELLED) {
            throw CoreException(ErrorType.CONFLICT, "취소된 주문은 결제를 진행할 수 없습니다.")
        }
        if (status == OrderStatus.PAID) {
            throw CoreException(ErrorType.CONFLICT, "이미 결제가 완료된 주문입니다.")
        }
        if (status == OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.CONFLICT, "이미 결제 처리 중인 주문입니다.")
        }
        this.status = OrderStatus.PAYMENT_PENDING
    }

    fun markPaid() {
        if (status == OrderStatus.CANCELLED) {
            throw CoreException(ErrorType.CONFLICT, "취소된 주문은 결제 완료 처리할 수 없습니다.")
        }
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.CONFLICT, "결제 대기 중인 주문만 결제 완료 처리할 수 있습니다.")
        }
        this.status = OrderStatus.PAID
    }

    fun markPaymentFailed() {
        if (status == OrderStatus.CANCELLED) {
            throw CoreException(ErrorType.CONFLICT, "취소된 주문은 결제 실패 처리할 수 없습니다.")
        }
        if (status == OrderStatus.PAID) {
            return
        }
        this.status = OrderStatus.PAYMENT_FAILED
    }

    fun cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw CoreException(ErrorType.ORDER_ALREADY_CANCELLED)
        }
        if (status == OrderStatus.PAYMENT_PENDING) {
            throw CoreException(ErrorType.CONFLICT, "결제 처리 중인 주문은 취소할 수 없습니다.")
        }
        if (status == OrderStatus.PAID) {
            throw CoreException(ErrorType.CONFLICT, "결제 완료된 주문은 취소할 수 없습니다.")
        }
        this.status = OrderStatus.CANCELLED
    }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.ORDER_NOT_OWNER)
        }
    }
}
