package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentRecoveryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 타임아웃 등으로 실패한 결제를 주기적으로 복구하는 스케줄러
 *
 * 1분마다 PENDING 상태의 Receipt을 확인하고,
 * PG에서 실제 결제 여부를 조회하여 시스템에 반영합니다.
 */
@Component
class PaymentRecoveryScheduler(
    private val paymentRecoveryService: PaymentRecoveryService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매 1분마다 실패한 결제를 복구합니다.
     *
     * 스케줄: 매 1분마다 실행
     * 지연 대기: 결제 실패 후 평균 1-2분 내 복구 시도
     */
    @Scheduled(fixedRate = 60000)
    fun recoverFailedPayments() {
        try {
            log.debug("Starting payment recovery job...")
            val recoveredCount = paymentRecoveryService.recoverFailedPayments()
            if (recoveredCount > 0) {
                log.info("Payment recovery job completed: $recoveredCount payments recovered")
            }
        } catch (e: Exception) {
            log.error("Payment recovery job failed", e)
        }
    }
}
