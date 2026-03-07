package com.loopers.application.order

import java.time.ZonedDateTime

data class CreateOrderCriteria(
    val items: List<CreateOrderItemCriteria>,
    val couponIssueId: Long? = null,
)

data class CreateOrderItemCriteria(
    val productId: Long,
    val quantity: Int,
)

data class GetOrdersCriteria(
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime,
)
