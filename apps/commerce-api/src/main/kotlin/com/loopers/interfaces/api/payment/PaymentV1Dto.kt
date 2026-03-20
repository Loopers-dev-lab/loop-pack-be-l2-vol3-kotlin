package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.payment.CardType

class PaymentV1Dto {
    data class PaymentRequest(
        val orderId: Long,
        val cardType: CardType,
        val cardNo: String,
    )

    data class CallbackRequest(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )

    data class PaymentResponse(
        val id: Long,
        val orderId: Long,
        val transactionKey: String?,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
        val createdAt: String,
    ) {
        companion object {
            fun from(paymentInfo: PaymentInfo): PaymentResponse {
                return PaymentResponse(
                    id = paymentInfo.id,
                    orderId = paymentInfo.orderId,
                    transactionKey = paymentInfo.transactionKey,
                    cardType = paymentInfo.cardType,
                    cardNo = paymentInfo.cardNo,
                    amount = paymentInfo.amount,
                    status = paymentInfo.status,
                    reason = paymentInfo.reason,
                    createdAt = paymentInfo.createdAt.toString(),
                )
            }
        }
    }
}
