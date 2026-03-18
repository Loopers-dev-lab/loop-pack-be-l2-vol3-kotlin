package com.loopers.domain.payment

interface ReceiptRepository {
    fun findByTransactionId(transactionId: String): Receipt?

    fun findByOrderId(orderId: Long): Receipt?

    fun findByTransactionIdForUpdate(transactionId: String): Receipt?

    fun findByOrderIdForUpdate(orderId: Long): Receipt?

    fun save(receipt: Receipt): Receipt
}
