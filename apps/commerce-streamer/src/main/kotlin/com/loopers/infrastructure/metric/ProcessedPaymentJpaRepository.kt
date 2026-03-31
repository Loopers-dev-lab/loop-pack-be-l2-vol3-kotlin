package com.loopers.infrastructure.metric

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessedPaymentJpaRepository : JpaRepository<ProcessedPaymentEntity, Long> {
    fun existsByPaymentId(paymentId: Long): Boolean
}
