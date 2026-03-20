package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentFacade
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PendingPaymentExpirationScheduler")
class PendingPaymentExpirationSchedulerTest {
    private val paymentFacade: PaymentFacade = mockk()
    private val scheduler = PendingPaymentExpirationScheduler(paymentFacade)

    @DisplayName("스케줄러가 실행되면 만료 대상 결제 수를 반환한다")
    @Test
    fun expiresPendingPayments_whenSchedulerRuns() {
        every { paymentFacade.expirePendingPayments(any()) } returns 3

        val expiredCount = scheduler.expirePendingPayments()

        assertThat(expiredCount).isEqualTo(3)
        verify(exactly = 1) { paymentFacade.expirePendingPayments(any()) }
    }
}
