package com.loopers.domain.payment.dto

import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class ReceiptInfo(
    val id: Long,
    val orderId: Long,
    val transactionId: String,
    val amount: BigDecimal,
    val status: ReceiptStatus,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(receipt: Receipt): ReceiptInfo =
            ReceiptInfo(
                id = receipt.id,
                orderId = receipt.orderId,
                transactionId = receipt.transactionId,
                amount = receipt.amount,
                status = receipt.status,
                createdAt = receipt.createdAt,
                updatedAt = receipt.updatedAt,
            )
    }
}
