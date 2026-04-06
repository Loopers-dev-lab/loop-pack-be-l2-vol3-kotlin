package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductRankingRepository {
    fun getRankedProducts(processingDate: LocalDate, page: Int, size: Int): List<ProductRankingReadModel>
    fun getRank(processingDate: LocalDate, productId: Long): Long?
    fun count(processingDate: LocalDate): Long
}
