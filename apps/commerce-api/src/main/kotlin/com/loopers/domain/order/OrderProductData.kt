package com.loopers.domain.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId

data class OrderProductData(
    val id: ProductId,
    val name: String,
    val price: Money,
)
