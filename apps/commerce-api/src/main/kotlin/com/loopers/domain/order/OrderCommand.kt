package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.user.User

data class CreateOrderCommand(
    val user: User,
    val products: List<Product>,
    val quantities: Map<Long, Int>,
    val brands: Map<Long, Brand>,
)
