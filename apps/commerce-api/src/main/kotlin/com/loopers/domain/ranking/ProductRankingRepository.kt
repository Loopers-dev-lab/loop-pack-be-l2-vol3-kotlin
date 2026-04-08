package com.loopers.domain.ranking

import java.time.LocalDate

data class RankedProductsWithCount(
    val products: List<ProductRankingReadModel>,
    val count: Long,
)

interface ProductRankingRepository {
    fun getRankedProducts(processingDate: LocalDate, page: Int, size: Int): List<ProductRankingReadModel>
    fun getRank(processingDate: LocalDate, productId: Long): Long?
    fun count(processingDate: LocalDate): Long
    fun getRankedProductsWithCount(processingDate: LocalDate, page: Int, size: Int): RankedProductsWithCount
}
