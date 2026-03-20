package com.loopers.interfaces.api.payment

import com.loopers.application.payment.GetPaymentResult
import com.loopers.application.payment.RequestPaymentResult
import com.loopers.application.payment.SyncPaymentResult
import com.loopers.domain.payment.PaymentStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.ZonedDateTime

class PaymentV1Dto {
    data class RequestPaymentRequest(
        @field:Min(value = 1, message = "주문 ID는 1 이상이어야 합니다.")
        val orderId: Long,
        @field:NotBlank(message = "카드 종류는 비어있을 수 없습니다.")
        val cardType: String,
        @field:NotBlank(message = "카드 번호는 비어있을 수 없습니다.")
        val cardNo: String,
    )

    data class CallbackRequest(
        val transactionKey: String,
        val status: String,
        val reason: String? = null,
    )

    data class RequestPaymentResponse(
        val paymentId: Long,
        val status: PaymentStatus,
        val transactionKey: String?,
    ) {
        companion object {
            fun from(result: RequestPaymentResult): RequestPaymentResponse {
                return RequestPaymentResponse(
                    paymentId = result.paymentId,
                    status = result.status,
                    transactionKey = result.transactionKey,
                )
            }
        }
    }

    data class PaymentResponse(
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
            fun from(result: GetPaymentResult): PaymentResponse {
                return PaymentResponse(
                    id = result.id,
                    orderId = result.orderId,
                    amount = result.amount,
                    status = result.status,
                    cardType = result.cardType,
                    cardNo = result.cardNo,
                    transactionKey = result.transactionKey,
                    failReason = result.failReason,
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class SyncPaymentResponse(
        val paymentId: Long,
        val status: PaymentStatus,
        val failReason: String?,
    ) {
        companion object {
            fun from(result: SyncPaymentResult): SyncPaymentResponse {
                return SyncPaymentResponse(
                    paymentId = result.paymentId,
                    status = result.status,
                    failReason = result.failReason,
                )
            }
        }
    }
}
