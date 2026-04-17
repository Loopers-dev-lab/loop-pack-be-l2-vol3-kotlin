package com.loopers.application.event

data class ProductViewedEvent(
    val userId: Long?,
    val productId: Long,
    val loginId: String?,
    val clientIp: String?,
    val userAgent: String?,
    val referer: String?,
)
