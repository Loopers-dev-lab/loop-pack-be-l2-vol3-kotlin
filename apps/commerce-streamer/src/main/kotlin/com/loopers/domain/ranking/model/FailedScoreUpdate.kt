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

    init {
        require(eventId.isNotBlank()) { "eventId는 공백일 수 없습니다. eventId=[$eventId]" }
        require(productId > 0) { "productId는 0보다 커야 합니다. productId=$productId" }
        require(!score.isNaN()) { "score는 NaN일 수 없습니다. score=$score" }
        require(!score.isInfinite()) { "score는 무한대일 수 없습니다. score=$score" }
        require(retryCount >= 0) { "retryCount는 0 이상이어야 합니다. retryCount=$retryCount" }
    }

    var retryCount: Int = retryCount
        private set

    fun incrementRetryCount() {
        retryCount++
    }
}
