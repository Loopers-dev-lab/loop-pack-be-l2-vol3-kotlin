package com.loopers.domain.ranking

interface ProductRankingRepository {
    fun incrementScore(
        productId: Long,
        increment: Double,
    )
}
