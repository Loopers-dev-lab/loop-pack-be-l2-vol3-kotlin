package com.loopers.domain.ranking

/**
 * 랭킹 가중치 Repository (Domain Layer).
 *
 * commerce-api는 Admin API를 위해 읽기와 쓰기 모두 지원한다.
 */
interface RankingWeightsRepository {

    /**
     * 현재 적용 중인 가중치를 조회한다.
     * 저장된 값이 없으면 null을 반환한다.
     */
    fun findCurrent(): RankingWeights?

    /**
     * 가중치를 저장한다.
     */
    fun save(weights: RankingWeights)
}
