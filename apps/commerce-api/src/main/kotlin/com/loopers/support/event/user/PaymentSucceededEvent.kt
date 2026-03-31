package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class PaymentSucceededEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
) : ApplicationEvent(paymentId)
