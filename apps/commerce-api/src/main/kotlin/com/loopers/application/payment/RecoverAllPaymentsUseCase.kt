package com.loopers.application.payment

import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RecoverAllPaymentsUseCase(
    private val paymentRepository: PaymentRepository,
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(): Int {
        val targetStatuses = listOf(PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT)
        val payments = paymentRepository.findByStatusIn(targetStatuses)
        var recoveredCount = 0
        for (payment in payments) {
            try {
                if (recoverPaymentUseCase.execute(payment.orderId)) recoveredCount++
            } catch (e: Exception) {
                log.warn("결제 복구 실패. orderId={}: {}", payment.orderId, e.message)
            }
        }
        return recoveredCount
    }
}
