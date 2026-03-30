package com.loopers.application.event

import java.math.BigDecimal

data class PaymentCompletedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val amount: BigDecimal,
)
