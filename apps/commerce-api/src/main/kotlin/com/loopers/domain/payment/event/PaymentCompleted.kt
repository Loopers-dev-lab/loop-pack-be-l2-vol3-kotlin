package com.loopers.domain.payment.event

import org.springframework.context.ApplicationEvent
import java.time.ZonedDateTime

class PaymentCompleted(
    source: Any = object : Any() {},
    val orderId: Long,
    val completedAt: ZonedDateTime = ZonedDateTime.now(),
) : ApplicationEvent(source)
