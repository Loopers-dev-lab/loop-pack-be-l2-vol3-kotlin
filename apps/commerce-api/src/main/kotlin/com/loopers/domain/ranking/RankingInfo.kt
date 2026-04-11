package com.loopers.domain.ranking

/**
 * 랭킹 정보 (1-based rank 포함).
 */
data class RankingInfo(
    val productId: Long,
    val rank: Long,
    val score: Double,
)

/**
 * 개별 상품의 순위 정보.
 * 랭킹에 없으면 rank, score 모두 null.
 */
data class ProductRankInfo(
    val rank: Long?,
    val score: Double?,
)

/**
 * 랭킹 페이지 조회 결과.
 */
data class RankingPageInfo(
    val rankings: List<RankingInfo>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
) {
    companion object {
        fun empty(page: Int, size: Int): RankingPageInfo =
            RankingPageInfo(rankings = emptyList(), totalCount = 0, page = page, size = size)
    }
}
