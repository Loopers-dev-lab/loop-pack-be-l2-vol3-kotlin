package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class PaymentReader(
    private val paymentRepository: PaymentRepository,
) {

    fun getById(id: Long): Payment =
        paymentRepository.findById(id)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

    fun getLatestByOrderId(orderId: Long, memberId: Long): Payment =
        paymentRepository.findLatestByOrderId(orderId, memberId)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

    fun findByTransactionKey(transactionKey: String): Payment? =
        paymentRepository.findByPgTransactionKey(transactionKey)
}
