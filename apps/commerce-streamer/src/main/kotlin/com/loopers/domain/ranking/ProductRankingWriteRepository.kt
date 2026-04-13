package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductRankingWriteRepository {
    fun incrementScore(processingDate: LocalDate, productId: Long, score: Double)
}
