package com.loopers.domain.payment.dto

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val transactionId: String,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(payment: Payment): PaymentInfo =
            PaymentInfo(
                id = payment.id,
                orderId = payment.orderId,
                transactionId = payment.transactionId,
                amount = payment.amount,
                status = payment.status,
                createdAt = payment.createdAt,
                updatedAt = payment.updatedAt,
            )
    }
}
