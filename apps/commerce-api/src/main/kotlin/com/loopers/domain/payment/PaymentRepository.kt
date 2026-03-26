package com.loopers.domain.payment

interface PaymentRepository {
    fun save(payment: Payment): Payment

    fun findById(id: Long): Payment?

    fun findLatestByOrderId(orderId: Long): Payment?

    fun findLatestByOrderId(orderId: Long, memberId: Long): Payment?

    fun findByPgTransactionKey(transactionKey: String): Payment?
}
