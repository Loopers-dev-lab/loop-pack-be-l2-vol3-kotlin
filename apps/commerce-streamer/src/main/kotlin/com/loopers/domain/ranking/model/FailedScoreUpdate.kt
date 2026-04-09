package com.loopers.domain.ranking.model

import java.time.LocalDate
import java.time.ZonedDateTime

class FailedScoreUpdate(
    val id: Long = 0,
    val eventId: String,
    val productId: Long,
    val score: Double,
    val rankingDate: LocalDate,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    retryCount: Int = 0,
) {

    var retryCount: Int = retryCount
        private set

    fun incrementRetryCount() {
        retryCount++
    }
}
