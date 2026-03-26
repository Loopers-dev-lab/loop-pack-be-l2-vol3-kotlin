package com.loopers.domain.payment

import com.loopers.domain.order.Order

enum class DisplayStatus {
    AWAITING_PAYMENT_RESULT,
    REQUIRES_REPAYMENT,
    ORDER_CONFIRMED,
    ;

    companion object {
        fun of(orderStatus: Order.Status, paymentStatus: Payment.Status): DisplayStatus = when {
            orderStatus == Order.Status.CREATED && paymentStatus == Payment.Status.SUCCESS -> ORDER_CONFIRMED
            orderStatus == Order.Status.PENDING && paymentStatus == Payment.Status.FAILED -> REQUIRES_REPAYMENT
            orderStatus == Order.Status.PENDING && paymentStatus == Payment.Status.PENDING -> AWAITING_PAYMENT_RESULT
            else -> throw IllegalStateException(
                "Unsupported display status combination: order=$orderStatus, payment=$paymentStatus",
            )
        }
    }
}
