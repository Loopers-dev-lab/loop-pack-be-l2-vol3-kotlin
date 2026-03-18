package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val amount: BigDecimal,
    val cardType: String,
    val cardNo: String,
    val transactionKey: String?,
    val status: PaymentStatus,
    val failReason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo {
            return PaymentInfo(
                id = payment.id,
                orderId = payment.orderId,
                amount = payment.amount,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                transactionKey = payment.transactionKey,
                status = payment.status,
                failReason = payment.failReason,
                createdAt = payment.createdAt,
            )
        }
    }
}
