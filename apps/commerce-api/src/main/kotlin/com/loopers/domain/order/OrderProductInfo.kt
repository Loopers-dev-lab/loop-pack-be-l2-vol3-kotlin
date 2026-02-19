package com.loopers.domain.order

import java.math.BigDecimal

data class OrderProductInfo(
    val id: Long,
    val name: String,
    val price: BigDecimal,
)
