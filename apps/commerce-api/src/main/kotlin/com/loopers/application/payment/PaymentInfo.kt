package com.loopers.application.payment

import com.loopers.domain.payment.model.Payment

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val transactionKey: String?,
    val status: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val reason: String?,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo = PaymentInfo(
            id = payment.id,
            orderId = payment.orderId,
            transactionKey = payment.transactionKey,
            status = payment.status.name,
            cardType = payment.cardType.name,
            cardNo = payment.cardNo,
            amount = payment.amount,
            reason = payment.reason,
        )
    }
}
