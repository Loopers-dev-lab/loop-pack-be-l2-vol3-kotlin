package com.loopers.domain.ranking

import java.time.LocalDate
import java.time.LocalDateTime

interface RankingRedisOperations {
    fun incrementScore(productId: Long, score: Double, date: LocalDate)
    fun incrementHourlyScore(productId: Long, score: Double, dateTime: LocalDateTime)
    fun carryOverScores(sourceDate: LocalDate, targetDate: LocalDate, weight: Double)
}
