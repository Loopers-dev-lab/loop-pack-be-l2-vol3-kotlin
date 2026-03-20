package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.payment.CardType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class PaymentDto {

    data class PaymentRequest(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
    ) {
        fun toCardType(): CardType = try {
            CardType.valueOf(cardType)
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 카드 유형입니다: $cardType")
        }
    }

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
