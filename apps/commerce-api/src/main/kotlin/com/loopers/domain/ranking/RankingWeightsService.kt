package com.loopers.domain.ranking

import org.springframework.stereotype.Component

/**
 * 랭킹 가중치 조회/변경 서비스.
 *
 * Admin API에서 호출된다.
 * commerce-streamer의 RankingProcessor는 이 서비스와 무관하게 Redis를 직접 읽는다.
 */
@Component
class RankingWeightsService(
    private val rankingWeightsRepository: RankingWeightsRepository,
) {

    /**
     * 현재 적용 중인 가중치를 조회한다.
     * 저장된 값이 없으면 DEFAULT를 반환한다.
     */
    fun getCurrent(): RankingWeights {
        return rankingWeightsRepository.findCurrent() ?: RankingWeights.DEFAULT
    }

    /**
     * 새 가중치를 저장한다.
     */
    fun update(weights: RankingWeights) {
        rankingWeightsRepository.save(weights)
    }
}
