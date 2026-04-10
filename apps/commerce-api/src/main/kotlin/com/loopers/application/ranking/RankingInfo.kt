package com.loopers.application.ranking

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel

data class RankingPageInfo(
    val content: List<RankingItemInfo>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
) {
    companion object {
        fun empty(page: Int, size: Int) = RankingPageInfo(emptyList(), 0, page, size)
    }
}

data class RankingItemInfo(
    val rank: Long,
    val score: Double,
    val product: RankingProductInfo,
)

data class RankingProductInfo(
    val id: Long,
    val name: String,
    val price: Long,
    val brandName: String,
) {
    companion object {
        fun of(product: ProductModel, brand: BrandModel) = RankingProductInfo(
            id = product.id,
            name = product.name,
            price = product.price,
            brandName = brand.name,
        )
    }
}
