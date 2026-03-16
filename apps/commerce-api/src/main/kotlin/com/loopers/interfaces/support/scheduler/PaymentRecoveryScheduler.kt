package com.loopers.interfaces.support.scheduler

import com.loopers.application.payment.RecoverAllPaymentsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentRecoveryScheduler(
    private val recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase,
) {
    @Scheduled(fixedDelay = 60000)
    fun recoverPendingPayments() {
        recoverAllPaymentsUseCase.execute()
    }
}
