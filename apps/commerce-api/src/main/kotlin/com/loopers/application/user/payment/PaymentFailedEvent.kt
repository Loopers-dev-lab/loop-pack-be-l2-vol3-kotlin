package com.loopers.application.user.payment

import com.loopers.domain.payment.PaymentReasonCode
import org.springframework.context.ApplicationEvent

class PaymentFailedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val reasonCode: PaymentReasonCode,
) : ApplicationEvent(paymentId)
