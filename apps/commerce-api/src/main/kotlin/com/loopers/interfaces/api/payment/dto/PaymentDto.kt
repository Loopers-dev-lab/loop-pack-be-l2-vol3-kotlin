package com.loopers.interfaces.api.payment.dto

import com.loopers.application.payment.PaymentInfo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

class PaymentDto {

    data class PaymentRequest(
        @field:Positive(message = "주문 ID는 양수여야 합니다.")
        val orderId: Long,
        @field:NotBlank(message = "카드 종류는 필수입니다.")
        val cardType: String,
        @field:NotBlank(message = "카드 번호는 필수입니다.")
        val cardNo: String,
    )

    data class PaymentResponse(
        val id: Long,
        val orderId: Long,
        val transactionKey: String?,
        val status: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val reason: String?,
    ) {
        companion object {
            fun from(info: PaymentInfo): PaymentResponse = PaymentResponse(
                id = info.id,
                orderId = info.orderId,
                transactionKey = info.transactionKey,
                status = info.status,
                cardType = info.cardType,
                cardNo = info.cardNo,
                amount = info.amount,
                reason = info.reason,
            )
        }
    }

    data class CallbackRequest(
        @field:Positive(message = "주문 ID는 양수여야 합니다.")
        val orderId: Long,
        @field:NotBlank(message = "트랜잭션 키는 필수입니다.")
        val transactionKey: String,
        @field:NotBlank(message = "상태는 필수입니다.")
        val status: String,
        val reason: String? = null,
    )

    data class RecoverResponse(
        val recoveredCount: Int,
    )

    data class SingleRecoverResponse(
        val recovered: Boolean,
    )
}
