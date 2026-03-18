package com.loopers.application.payment

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentRecoveryScheduler(
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun recover() {
        log.info("결제 복구 스케줄러 실행")
        recoverPaymentUseCase.recoverPendingPayments()
        log.info("결제 복구 스케줄러 완료")
    }
}
