package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.payment.CardType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

class PaymentV1Dto {
    data class PaymentRequest(
        @field:NotNull(message = "주문 ID는 필수입니다.")
        val orderId: Long,
        @field:NotNull(message = "카드 종류는 필수입니다.")
        val cardType: CardType,
        @field:NotBlank(message = "카드 번호는 필수입니다.")
        val cardNo: String,
        @field:Min(value = 1, message = "결제 금액은 1 이상이어야 합니다.")
        val amount: Long,
    )

    data class PaymentResponse(
        val paymentId: Long,
        val orderId: Long,
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
            fun from(info: PaymentInfo): PaymentResponse = PaymentResponse(
                paymentId = info.id,
                orderId = info.orderId,
                transactionKey = info.transactionKey,
                cardType = info.cardType,
                cardNo = info.cardNo,
                amount = info.amount,
                status = info.status,
                failReason = info.failReason,
                requestedAt = info.requestedAt,
                completedAt = info.completedAt,
            )
        }
    }

    data class CallbackRequest(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )
}
