package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentFacade
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PendingPaymentExpirationScheduler(
    private val paymentFacade: PaymentFacade,
) {
    @Scheduled(fixedDelayString = "PT1M")
    fun expirePendingPayments(): Int {
        return paymentFacade.expirePendingPayments(ZonedDateTime.now())
    }
}
