package com.loopers.kafka

import java.time.ZonedDateTime

data class ProductLikedPayload(
    val likeId: Long,
    val productId: Long,
    val memberId: Long,
    val delta: Long = 1L,
)

data class ProductUnlikedPayload(
    val likeId: Long,
    val productId: Long,
    val memberId: Long,
    val delta: Long = -1L,
)

data class ProductViewedPayload(
    val productId: Long,
    val memberId: Long?,
)

data class OrderPaidPayload(
    val orderId: Long,
    val memberId: Long,
    val items: List<OrderPaidItemPayload>,
)

data class OrderPaidItemPayload(
    val productId: Long,
    val quantity: Int,
)

data class CouponIssueRequestedPayload(
    val requestId: Long,
    val couponId: Long,
    val memberId: Long,
    val requestedAt: ZonedDateTime,
)
