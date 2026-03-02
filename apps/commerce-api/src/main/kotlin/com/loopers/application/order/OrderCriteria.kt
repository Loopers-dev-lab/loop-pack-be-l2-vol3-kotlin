package com.loopers.application.order

import java.time.ZonedDateTime

data class CreateOrderCriteria(
    val items: List<CreateOrderItemCriteria>,
)

data class CreateOrderItemCriteria(
    val productId: Long,
    val quantity: Int,
)

data class GetOrdersCriteria(
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime,
)
