package com.loopers.application.payment

data class PaymentCriteria(
    val orderId: Long,
    val cardType: String,
    val cardNo: String,
)
