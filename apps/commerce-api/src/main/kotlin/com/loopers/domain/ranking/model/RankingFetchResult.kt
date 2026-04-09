package com.loopers.domain.ranking.model

/**
 * Redis getTopN 조회 결과.
 * - entries: 파싱 성공한 랭킹 항목
 * - rawFetchCount: Redis에서 실제로 소비한 항목 수 (파싱 실패 포함)
 *
 * offset 계산과 종료 조건은 rawFetchCount 기준으로 수행해야
 * 파싱 드랍으로 인한 커서 어긋남을 방지할 수 있다.
 */
data class RankingFetchResult(
    val entries: List<RankingEntry>,
    val rawFetchCount: Int,
)
