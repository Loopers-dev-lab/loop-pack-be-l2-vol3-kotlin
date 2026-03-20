package com.loopers.application.user.payment

class PaymentCreateCommand(
    val userId: Long,
    val orderId: Long,
    val idempotencyKey: String,
    val cardType: String,
    val cardNo: String,
)
