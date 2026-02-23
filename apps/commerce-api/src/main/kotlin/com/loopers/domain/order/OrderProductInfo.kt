package com.loopers.domain.order

import com.loopers.domain.common.Money

data class OrderProductInfo(
    val id: Long,
    val name: String,
    val price: Money,
)
