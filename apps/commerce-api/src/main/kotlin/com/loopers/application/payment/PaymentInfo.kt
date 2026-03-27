package com.loopers.application.payment

import com.loopers.domain.payment.Payment

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val transactionKey: String?,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val failureReason: String?,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo {
            return PaymentInfo(
                id = requireNotNull(payment.persistenceId),
                orderId = payment.refOrderId,
                userId = payment.refUserId,
                transactionKey = payment.transactionKey,
                cardType = payment.cardType.name,
                cardNo = payment.cardNo,
                amount = payment.amount,
                status = payment.status.name,
                failureReason = payment.failureReason,
            )
        }
    }
}
