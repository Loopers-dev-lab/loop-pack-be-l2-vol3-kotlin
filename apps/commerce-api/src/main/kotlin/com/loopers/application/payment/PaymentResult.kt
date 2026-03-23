package com.loopers.application.payment

import com.loopers.domain.Money
import com.loopers.domain.payment.PaymentInfo
import java.time.ZonedDateTime

data class PaymentResult(
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
        fun from(info: PaymentInfo): PaymentResult {
            return PaymentResult(
                id = info.id,
                orderId = info.orderId,
                userId = info.userId,
                transactionKey = info.transactionKey,
                amount = info.amount,
                cardType = info.cardType,
                cardNo = info.cardNo,
                paymentStatus = info.paymentStatus,
                failReason = info.failReason,
                createdAt = info.createdAt,
            )
        }
    }
}
