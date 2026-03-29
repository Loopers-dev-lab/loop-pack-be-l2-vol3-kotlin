package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentFacade
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class PaymentRecoveryScheduler(
    private val paymentFacade: PaymentFacade,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * REQUESTED, TIMEOUT 상태인 결제건을 주기적으로 PG에 확인하여 상태를 동기화한다.
     * fixedDelay: 이전 실행 완료 후 60초 대기 → 다음 실행
     */
//    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    fun recoverPendingPayments() {
        log.info("결제 복구 스케줄러 실행")
        paymentFacade.recoverPendingPayments()
    }
}
