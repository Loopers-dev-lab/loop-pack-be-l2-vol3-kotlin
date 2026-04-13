package com.loopers.domain.ranking.model

/**
 * 랭킹 항목 도메인 모델.
 * Redis ZSET의 (member, score) 결과를 도메인 언어로 표현한다.
 * 순위(rank)는 UseCase에서 offset + index로 계산하므로 이 모델에 포함하지 않는다.
 */
data class RankingEntry(
    val productId: Long,
    val score: Double,
)
