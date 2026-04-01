package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val transactionKey: String?,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo {
            return PaymentInfo(
                id = payment.id,
                orderId = payment.orderId,
                transactionKey = payment.transactionKey,
                cardType = payment.cardType.name,
                cardNo = payment.cardNo,
                amount = payment.amount,
                status = payment.status.name,
                reason = payment.reason,
                createdAt = payment.createdAt,
            )
        }
    }
}
