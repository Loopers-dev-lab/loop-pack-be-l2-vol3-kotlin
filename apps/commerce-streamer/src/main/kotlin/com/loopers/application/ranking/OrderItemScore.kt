package com.loopers.application.ranking

import java.math.BigDecimal

data class OrderItemScore(
    val productId: Long,
    val amount: BigDecimal,
)
