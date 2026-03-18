package com.loopers.interfaces.support.scheduler

import com.loopers.application.payment.RecoverAllPaymentsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [CR 미반영] 분산 락(ShedLock) 미적용:
// 현재 단일 인스턴스 배포이며, RecoverPaymentUseCase가 비관적 락으로 상태 변경 중복을 방지한다.
// 스케일아웃 시 ShedLock 또는 Leader Election 도입 필요.
@Component
class PaymentRecoveryScheduler(
    private val recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase,
) {
    @Scheduled(fixedDelay = 60000)
    fun recoverPendingPayments() {
        recoverAllPaymentsUseCase.execute()
    }
}
