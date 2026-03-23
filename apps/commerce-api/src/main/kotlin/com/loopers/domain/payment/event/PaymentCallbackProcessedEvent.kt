package com.loopers.domain.payment.event

import org.springframework.context.ApplicationEvent
import java.time.ZonedDateTime

class PaymentCallbackProcessedEvent(
    source: Any = object : Any() {},
    val orderId: Long,
    val status: String,
    val processedAt: ZonedDateTime = ZonedDateTime.now(),
) : ApplicationEvent(source)
