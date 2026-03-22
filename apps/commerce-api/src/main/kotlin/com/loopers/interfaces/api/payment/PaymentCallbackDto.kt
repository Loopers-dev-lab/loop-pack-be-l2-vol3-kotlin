package com.loopers.interfaces.api.payment

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

object PaymentCallbackDto {
    data class CallbackRequest(
        @field:NotBlank
        val transactionKey: String,

        @field:NotBlank
        val orderId: String,

        @field:NotNull
        val cardType: CardType,

        @field:NotBlank
        val cardNo: String,

        @field:Positive
        val amount: Long,

        @field:NotNull
        val status: TransactionStatus,

        val reason: String?,
    )
}
