package com.loopers.domain.order.dto

import com.loopers.domain.product.Product
import java.math.BigDecimal

/**
 * OrderItem 생성용 DTO
 * Order.createWithItems()에 전달되는 중간 데이터 구조
 */
data class OrderItemSpec(
    val product: Product,
    val quantity: Int,
    val price: BigDecimal,
)
