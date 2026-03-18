package com.loopers.domain.payment

interface ReceiptRepository {
    fun findById(id: Long): Receipt?

    fun findByTransactionId(transactionId: String): Receipt?

    fun findByOrderId(orderId: Long): Receipt?

    fun findByTransactionIdForUpdate(transactionId: String): Receipt?

    fun findByOrderIdForUpdate(orderId: Long): Receipt?

    fun findByStatusAndCreatedAtBefore(status: ReceiptStatus, before: java.time.ZonedDateTime): List<Receipt>

    fun save(receipt: Receipt): Receipt
}
