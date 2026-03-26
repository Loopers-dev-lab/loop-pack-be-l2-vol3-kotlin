package com.loopers.domain.payment

import java.math.BigDecimal
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val cardType: String,
    val cardNo: String,
    val transactionKey: String?,
    val failReason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(model: PaymentModel): PaymentInfo {
            return PaymentInfo(
                id = model.id,
                orderId = model.orderId,
                userId = model.userId,
                amount = model.amount,
                status = model.status,
                cardType = model.cardType,
                cardNo = model.cardNo,
                transactionKey = model.transactionKey,
                failReason = model.failReason,
                createdAt = model.createdAt,
            )
        }
    }
}
