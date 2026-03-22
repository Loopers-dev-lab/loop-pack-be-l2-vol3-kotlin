package com.loopers.domain.order.dto

import java.math.BigDecimal

data class OrderInfo(
    val orderId: Long,
    val amount: BigDecimal,
)
