package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus

data class PaymentInfo(
    val paymentId: Long,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: PaymentStatus,
    val transactionKey: String?,
    val failReason: String?,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo {
            return PaymentInfo(
                paymentId = payment.id,
                orderId = payment.orderId,
                cardType = payment.cardType.name,
                cardNo = payment.cardNo,
                amount = payment.amount,
                status = payment.status,
                transactionKey = payment.transactionKey,
                failReason = payment.failReason,
            )
        }
    }
}
