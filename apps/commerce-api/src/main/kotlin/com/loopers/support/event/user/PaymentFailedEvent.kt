package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class PaymentFailedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val reasonCode: String,
) : ApplicationEvent(paymentId)
