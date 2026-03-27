package com.loopers.application.payment

data class RequestPaymentCommand(
    val orderId: Long,
    val cardType: String,
    val cardNo: String,
)
