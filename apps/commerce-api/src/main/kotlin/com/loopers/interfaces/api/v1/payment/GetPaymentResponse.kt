package com.loopers.interfaces.api.v1.payment

import com.loopers.application.payment.PaymentInfo

data class GetPaymentResponse(
    val id: Long,
    val orderId: Long,
    val transactionKey: String?,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val failureReason: String?,
) {
    companion object {
        fun from(info: PaymentInfo): GetPaymentResponse {
            return GetPaymentResponse(
                id = info.id,
                orderId = info.orderId,
                transactionKey = info.transactionKey,
                cardType = info.cardType,
                cardNo = info.cardNo,
                amount = info.amount,
                status = info.status,
                failureReason = info.failureReason,
            )
        }
    }
}
