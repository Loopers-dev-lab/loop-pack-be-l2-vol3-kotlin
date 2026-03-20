package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo
import com.loopers.application.payment.PaymentUseCase
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PgPaymentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class PaymentV1Dto {

    data class Request(
        @field:NotNull val orderId: Long?,
        @field:NotNull val cardType: CardType?,
        @field:NotBlank val cardNo: String,
    ) {
        fun toCommand() = PaymentUseCase.RequestCommand(
            orderId = requireNotNull(orderId),
            cardType = requireNotNull(cardType),
            cardNo = cardNo,
        )
    }

    data class DetailResponse(
        val paymentId: Long,
        val orderId: Long,
        val orderStatus: String,
        val paymentStatus: String,
        val amount: Long,
        val cardType: CardType,
        val cardNo: String,
        val transactionKey: String?,
        val reason: String?,
    ) {
        companion object {
            fun from(info: PaymentInfo.Detail) = DetailResponse(
                paymentId = info.paymentId,
                orderId = info.orderId,
                orderStatus = info.orderStatus,
                paymentStatus = info.paymentStatus,
                amount = info.amount,
                cardType = info.cardType,
                cardNo = info.cardNo,
                transactionKey = info.transactionKey,
                reason = info.reason,
            )
        }
    }

    data class CallbackRequest(
        @field:NotBlank val transactionKey: String,
        @field:NotBlank val orderId: String,
        @field:NotNull val cardType: CardType?,
        @field:NotBlank val cardNo: String,
        @field:NotNull val amount: Long?,
        @field:NotNull val status: PgPaymentStatus?,
        val reason: String?,
    ) {
        fun toCommand() = PaymentUseCase.CallbackCommand(
            transactionKey = transactionKey,
            status = requireNotNull(status),
            reason = reason,
        )
    }
}
