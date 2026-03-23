package com.loopers.domain.payment

import com.loopers.domain.Money

data class CreatePaymentCommand(
    val orderId: Long,
    val userId: Long,
    val amount: Money,
    val cardType: String,
    val cardNo: String,
)
