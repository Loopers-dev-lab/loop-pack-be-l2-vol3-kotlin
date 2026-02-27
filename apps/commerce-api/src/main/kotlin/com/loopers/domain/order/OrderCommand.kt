package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product

data class CreateOrderCommand(
    val userId: Long,
    val products: List<Product>,
    val quantities: Map<Long, Int>,
    val brands: Map<Long, Brand>,
)
