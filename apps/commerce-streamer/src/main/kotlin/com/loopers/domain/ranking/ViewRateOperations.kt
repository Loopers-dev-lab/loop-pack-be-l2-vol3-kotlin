package com.loopers.domain.ranking

import java.time.LocalDateTime

interface ViewRateOperations {
    fun incrementAndGetRequestCount(identifier: String, dateTime: LocalDateTime): Long
    fun addViewedProductAndGetCount(identifier: String, productId: Long, dateTime: LocalDateTime): Long
}
