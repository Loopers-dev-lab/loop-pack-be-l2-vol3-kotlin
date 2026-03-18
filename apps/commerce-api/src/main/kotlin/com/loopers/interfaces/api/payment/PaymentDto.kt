package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo

class PaymentDto {

    data class PaymentRequest(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
    )

    data class CallbackRequest(
        val transactionKey: String,
        val orderId: String,
        val status: String,
        val reason: String?,
    )

    data class PaymentResponse(
        val paymentId: Long,
        val orderId: String,
        val cardType: String,
        val amount: Long,
        val status: String,
        val transactionKey: String?,
    ) {
        companion object {
            fun from(info: PaymentInfo): PaymentResponse {
                return PaymentResponse(
                    paymentId = info.paymentId,
                    orderId = info.orderId,
                    cardType = info.cardType,
                    amount = info.amount,
                    status = info.status.name,
                    transactionKey = info.transactionKey,
                )
            }
        }
    }
}
