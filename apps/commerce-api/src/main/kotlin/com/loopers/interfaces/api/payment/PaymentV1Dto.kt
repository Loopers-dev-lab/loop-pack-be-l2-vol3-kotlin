package com.loopers.interfaces.api.payment

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

object PaymentV1Dto {

    data class CreatePaymentRequest(
        @field:NotNull(message = "주문 ID는 필수입니다")
        val orderId: Long,

        @field:NotBlank(message = "카드 유형은 필수입니다")
        val cardType: String,

        @field:NotBlank(message = "카드 번호는 필수입니다")
        val cardNo: String,
    )

    data class PaymentResponse(
        val paymentId: Long,
        val orderId: Long,
        val amount: String,
        val status: String,
        val cardType: String,
        val cardNo: String,
    )
}
