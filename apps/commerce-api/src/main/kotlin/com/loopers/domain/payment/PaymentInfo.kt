package com.loopers.domain.payment

import com.loopers.domain.Money
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val transactionKey: String?,
    val amount: Money,
    val cardType: String,
    val cardNo: String,
    val paymentStatus: String,
    val failReason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo {
            return PaymentInfo(
                id = payment.id,
                orderId = payment.orderId,
                userId = payment.userId,
                transactionKey = payment.transactionKey,
                amount = payment.amount,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                paymentStatus = payment.paymentStatus.name,
                failReason = payment.failReason,
                createdAt = payment.createdAt,
            )
        }
    }
}
