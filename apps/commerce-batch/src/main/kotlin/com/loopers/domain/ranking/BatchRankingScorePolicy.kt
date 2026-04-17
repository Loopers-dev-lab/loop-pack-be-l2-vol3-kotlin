package com.loopers.domain.ranking

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 주간/월간 랭킹 배치의 가중치 정책.
 *
 * streamer 의 `RankingScorePolicy` 와 **동일한 상수 집합**을 유지하여
 * 실시간 ZSET 점수와 MV 점수의 의미가 일관되도록 한다.
 *
 * 프로젝트가 이미 `RankingKeyGenerator` 를 api/batch 에서 중복 정의하는 선례를 따른다.
 * 실제 상수 값은 `application.yml` 의 `ranking.weight.*` 에서 주입된다.
 */
@Component
class BatchRankingScorePolicy(
    @Value("\${ranking.weight.view:0.1}") val viewWeight: Double,
    @Value("\${ranking.weight.like:0.2}") val likeWeight: Double,
    @Value("\${ranking.weight.order:0.7}") val orderWeight: Double,
)
