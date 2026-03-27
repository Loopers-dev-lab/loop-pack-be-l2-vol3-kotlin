package com.loopers.application.user.payment

import org.springframework.context.ApplicationEvent

class PaymentSucceededEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
) : ApplicationEvent(paymentId)
