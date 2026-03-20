package com.loopers.application.payment

import com.loopers.domain.payment.PaymentInfo
import com.loopers.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class RequestPaymentResult(
    val paymentId: Long,
    val status: PaymentStatus,
    val transactionKey: String?,
) {
    companion object {
        fun from(info: PaymentInfo): RequestPaymentResult {
            return RequestPaymentResult(
                paymentId = info.id,
                status = info.status,
                transactionKey = info.transactionKey,
            )
        }
    }
}

data class SyncPaymentResult(
    val paymentId: Long,
    val status: PaymentStatus,
    val failReason: String?,
) {
    companion object {
        fun from(info: PaymentInfo): SyncPaymentResult {
            return SyncPaymentResult(
                paymentId = info.id,
                status = info.status,
                failReason = info.failReason,
            )
        }
    }
}

data class GetPaymentResult(
    val id: Long,
    val orderId: Long,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val cardType: String,
    val cardNo: String,
    val transactionKey: String?,
    val failReason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: PaymentInfo): GetPaymentResult {
            return GetPaymentResult(
                id = info.id,
                orderId = info.orderId,
                amount = info.amount,
                status = info.status,
                cardType = info.cardType,
                cardNo = info.cardNo,
                transactionKey = info.transactionKey,
                failReason = info.failReason,
                createdAt = info.createdAt,
            )
        }
    }
}
