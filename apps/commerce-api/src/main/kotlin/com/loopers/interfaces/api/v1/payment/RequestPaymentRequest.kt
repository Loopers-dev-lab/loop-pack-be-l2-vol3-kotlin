package com.loopers.interfaces.api.v1.payment

import com.loopers.application.payment.RequestPaymentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class RequestPaymentRequest(
    @field:NotNull(message = "주문 ID는 필수입니다.")
    val orderId: Long,

    @field:NotBlank(message = "카드 타입은 필수입니다.")
    val cardType: String,

    @field:NotBlank(message = "카드 번호는 필수입니다.")
    val cardNo: String,
) {
    fun toCommand() = RequestPaymentCommand(
        orderId = orderId,
        cardType = cardType,
        cardNo = cardNo,
    )
}
