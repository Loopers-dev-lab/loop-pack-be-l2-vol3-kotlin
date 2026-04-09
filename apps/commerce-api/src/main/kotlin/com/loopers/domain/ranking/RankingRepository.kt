package com.loopers.domain.ranking

interface RankingRepository {

    /**
     * 특정 날짜의 랭킹을 페이지 단위로 조회한다.
     * @return (productId, score) 쌍의 리스트 (score 내림차순)
     */
    fun getTopRankings(date: String, offset: Long, size: Long): List<RankingEntry>

    /**
     * 특정 날짜에 해당 상품의 순위를 조회한다.
     * @return 0-based 순위 (없으면 null)
     */
    fun getRank(date: String, productId: Long): Long?

    /**
     * 특정 날짜에 해당 상품의 점수를 조회한다.
     */
    fun getScore(date: String, productId: Long): Double?

    /**
     * 특정 날짜의 전체 랭킹 수를 조회한다.
     */
    fun getTotalCount(date: String): Long
}

data class RankingEntry(
    val productId: Long,
    val score: Double,
    val rank: Long,
)
