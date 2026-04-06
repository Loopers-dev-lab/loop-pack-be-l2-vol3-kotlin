package com.loopers.application.orderqueue

data class EnterQueueCriteria(
    val loginId: String,
)

data class GetQueuePositionCriteria(
    val loginId: String,
)
