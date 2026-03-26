package com.loopers.application.payment

import com.loopers.domain.payment.PaymentModel
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val memberId: Long,
    val transactionKey: String?,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val failReason: String?,
    val requestedAt: ZonedDateTime,
    val completedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: PaymentModel): PaymentInfo = PaymentInfo(
            id = model.id,
            orderId = model.orderId,
            memberId = model.memberId,
            transactionKey = model.transactionKey,
            cardType = model.cardType.name,
            cardNo = model.cardNo,
            amount = model.amount,
            status = model.status.name,
            failReason = model.failReason,
            requestedAt = model.requestedAt,
            completedAt = model.completedAt,
        )
    }
}
