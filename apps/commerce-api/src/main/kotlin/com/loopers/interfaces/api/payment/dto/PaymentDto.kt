package com.loopers.interfaces.api.payment.dto

import com.loopers.application.payment.PaymentInfo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive

class PaymentDto {

    data class PaymentRequest(
        @field:Positive(message = "주문 ID는 양수여야 합니다.")
        val orderId: Long,
        @field:NotBlank(message = "카드 종류는 필수입니다.")
        @field:Pattern(regexp = "^(SAMSUNG|KB|HYUNDAI)$", message = "카드 종류는 SAMSUNG, KB, HYUNDAI 중 하나여야 합니다.")
        val cardType: String,
        @field:NotBlank(message = "카드 번호는 필수입니다.")
        val cardNo: String,
    ) {
        override fun toString(): String =
            "PaymentRequest(orderId=$orderId, cardType=$cardType, cardNo=****)"
    }

    data class PaymentResponse(
        val id: Long,
        val orderId: Long,
        val transactionKey: String?,
        val status: String,
        val cardType: String,
        val maskedCardNo: String,
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
                maskedCardNo = info.maskedCardNo,
                amount = info.amount,
                reason = info.reason,
            )
        }
    }

    enum class CallbackStatus { SUCCESS, FAILED }

    data class CallbackRequest(
        @field:Positive(message = "주문 ID는 양수여야 합니다.")
        val orderId: Long,
        @field:NotBlank(message = "트랜잭션 키는 필수입니다.")
        val transactionKey: String,
        val status: CallbackStatus,
        val reason: String? = null,
    )

    data class RecoverResponse(
        val recoveredCount: Int,
    )

    data class SingleRecoverResponse(
        val recovered: Boolean,
    )
}
