package com.loopers.infrastructure.metric

import com.loopers.domain.metric.ProcessedPaymentRepository
import org.springframework.stereotype.Repository

@Repository
class ProcessedPaymentRepositoryImpl(
    private val processedPaymentJpaRepository: ProcessedPaymentJpaRepository,
) : ProcessedPaymentRepository {
    override fun existsByPaymentId(paymentId: Long): Boolean =
        processedPaymentJpaRepository.existsByPaymentId(paymentId)

    override fun save(paymentId: Long) {
        processedPaymentJpaRepository.saveAndFlush(ProcessedPaymentEntity(paymentId = paymentId))
    }
}
