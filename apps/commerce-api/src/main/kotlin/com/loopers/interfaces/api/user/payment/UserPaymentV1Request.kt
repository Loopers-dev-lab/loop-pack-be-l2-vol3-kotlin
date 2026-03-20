package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.payment.PaymentCreateCommand
import jakarta.validation.constraints.NotBlank

class UserPaymentV1Request {

    data class Create(
        val orderId: Long,
        @field:NotBlank
        val cardType: String,
        @field:NotBlank
        val cardNo: String,
    ) {
        fun toCommand(userId: Long, idempotencyKey: String): PaymentCreateCommand =
            PaymentCreateCommand(
                userId = userId,
                orderId = orderId,
                idempotencyKey = idempotencyKey,
                cardType = cardType,
                cardNo = cardNo,
            )
    }
}
