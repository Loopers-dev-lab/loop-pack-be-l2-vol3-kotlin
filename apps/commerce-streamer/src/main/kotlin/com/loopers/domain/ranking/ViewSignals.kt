package com.loopers.domain.ranking

data class ViewSignals(
    val isLoggedIn: Boolean,
    val hasUserAgent: Boolean,
    val hasReferer: Boolean,
    val requestsPerMinute: Long,
    val distinctProductsIn10Min: Long,
)
