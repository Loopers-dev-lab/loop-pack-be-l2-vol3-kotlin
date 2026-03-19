package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentResult
import com.loopers.domain.Money
import java.time.ZonedDateTime

class PaymentAdminV1Dto {

    data class PaymentAdminResponse(
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
            fun from(result: PaymentResult): PaymentAdminResponse {
                return PaymentAdminResponse(
                    id = result.id,
                    orderId = result.orderId,
                    userId = result.userId,
                    transactionKey = result.transactionKey,
                    amount = result.amount,
                    cardType = result.cardType,
                    cardNo = result.cardNo,
                    paymentStatus = result.paymentStatus,
                    failReason = result.failReason,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
