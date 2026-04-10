package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingScoreConfig
import com.loopers.domain.ranking.RankingScoreConfigRepository
import org.springframework.stereotype.Component

@Component
class RankingScoreConfigRepositoryImpl(
    private val rankingScoreConfigJpaRepository: RankingScoreConfigJpaRepository,
) : RankingScoreConfigRepository {

    override fun findAll(): List<RankingScoreConfig> {
        return rankingScoreConfigJpaRepository.findAll()
    }
}
