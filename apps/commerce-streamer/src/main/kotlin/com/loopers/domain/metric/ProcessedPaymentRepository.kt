package com.loopers.domain.metric

interface ProcessedPaymentRepository {
    fun existsByPaymentId(paymentId: Long): Boolean

    fun save(paymentId: Long)
}
