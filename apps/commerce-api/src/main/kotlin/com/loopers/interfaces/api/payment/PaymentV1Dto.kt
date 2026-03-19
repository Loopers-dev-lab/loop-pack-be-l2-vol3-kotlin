package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCallbackCriteria
import com.loopers.application.payment.PaymentResult
import com.loopers.application.payment.RequestPaymentCriteria
import com.loopers.domain.Money
import java.time.ZonedDateTime

class PaymentV1Dto {

    data class RequestPaymentRequest(
        val orderId: Long,
        val cardType: String,
        val cardNo: String,
    ) {
        fun toCriteria(): RequestPaymentCriteria {
            return RequestPaymentCriteria(
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
            )
        }
    }

    data class PaymentCallbackRequest(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    ) {
        fun toCriteria(): PaymentCallbackCriteria {
            return PaymentCallbackCriteria(
                transactionKey = transactionKey,
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                status = status,
                reason = reason,
            )
        }
    }

    data class PaymentResponse(
        val id: Long,
        val orderId: Long,
        val transactionKey: String?,
        val amount: Money,
        val cardType: String,
        val cardNo: String,
        val paymentStatus: String,
        val failReason: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: PaymentResult): PaymentResponse {
                return PaymentResponse(
                    id = result.id,
                    orderId = result.orderId,
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
