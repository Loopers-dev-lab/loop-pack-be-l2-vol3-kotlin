package com.loopers.application.order

import java.time.LocalDate

data class CreateOrderCriteria(
    val loginId: String,
    val items: List<CreateOrderItemCriteria>,
    val couponId: Long? = null,
)

data class CreateOrderItemCriteria(
    val productId: Long,
    val quantity: Int,
)

data class GetOrdersCriteria(
    val loginId: String,
    val startAt: LocalDate,
    val endAt: LocalDate,
    val page: Int,
    val size: Int,
)

data class GetOrderCriteria(
    val loginId: String,
    val orderId: Long,
)

data class CancelOrderCriteria(
    val loginId: String,
    val orderId: Long,
)
