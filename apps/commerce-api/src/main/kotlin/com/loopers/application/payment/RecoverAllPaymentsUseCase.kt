package com.loopers.application.payment

import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class RecoveryResult(val attempted: Int, val recovered: Int)

@Component
class RecoverAllPaymentsUseCase(
    private val paymentRepository: PaymentRepository,
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
    @Value("\${payment.recovery.batch-size:50}")
    private val batchSize: Int = 50,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(): RecoveryResult {
        val targetStatuses = listOf(PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT)
        val payments = paymentRepository.findByStatusIn(targetStatuses, limit = batchSize)
        var recoveredCount = 0
        for (payment in payments) {
            try {
                if (recoverPaymentUseCase.execute(payment.orderId)) recoveredCount++
            } catch (e: CoreException) {
                log.warn("결제 복구 실패. orderId={}, errorType={}", payment.orderId, e.errorType)
            } catch (e: Exception) {
                log.error("결제 복구 중 예상치 못한 오류. orderId={}", payment.orderId, e)
            }
        }
        log.info("결제 복구 배치 완료. attempted={}, recovered={}", payments.size, recoveredCount)
        return RecoveryResult(attempted = payments.size, recovered = recoveredCount)
    }
}
