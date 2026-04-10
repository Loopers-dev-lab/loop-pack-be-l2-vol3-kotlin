package com.loopers.domain.ranking

/**
 * 랭킹 가중치 Repository (Domain Layer).
 *
 * commerce-streamer는 읽기 전용이다.
 * 쓰기는 commerce-api의 Admin API를 통해서만 수행한다.
 */
interface RankingWeightsRepository {

    /**
     * 현재 적용 중인 가중치를 조회한다.
     * 저장된 값이 없거나 조회 실패 시 null을 반환한다.
     */
    fun findCurrent(): RankingWeights?
}
