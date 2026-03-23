package com.loopers.domain.payment.event

import org.springframework.context.ApplicationEvent
import java.time.ZonedDateTime

class PaymentRequestedEvent(
    source: Any = object : Any() {},
    val userId: Long,
    val orderId: Long,
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
) : ApplicationEvent(source)
