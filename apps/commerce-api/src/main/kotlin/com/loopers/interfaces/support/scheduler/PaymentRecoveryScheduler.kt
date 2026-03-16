package com.loopers.interfaces.support.scheduler

import com.loopers.application.payment.RecoverPaymentUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentRecoveryScheduler(
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
) {
    // TODO: 병렬 처리 전환
    @Scheduled(fixedDelay = 60000)
    fun recoverPendingPayments() {
        recoverPaymentUseCase.recoverAll()
    }
}
