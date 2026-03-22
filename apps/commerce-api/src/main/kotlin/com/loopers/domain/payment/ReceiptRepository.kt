package com.loopers.domain.payment

import java.time.LocalDateTime
import java.time.ZonedDateTime

interface ReceiptRepository {
    fun findById(id: Long): Receipt?

    fun findByTransactionId(transactionId: String): Receipt?

    fun findByOrderId(orderId: Long): Receipt?

    fun findByTransactionIdForUpdate(transactionId: String): Receipt?

    fun findByOrderIdForUpdate(orderId: Long): Receipt?

    fun findByIdForUpdate(id: Long): Receipt?

    fun findByStatusAndCreatedAtBefore(status: ReceiptStatus, before: ZonedDateTime): List<Receipt>

    fun findPendingReceiptsCreatedBefore(before: LocalDateTime): List<Receipt>

    fun findReceiptsForRecovery(statuses: List<ReceiptStatus>, before: LocalDateTime): List<Receipt>

    fun save(receipt: Receipt): Receipt
}
