package com.loopers.domain.ranking

interface RankingScoreConfigRepository {
    fun findAll(): List<RankingScoreConfig>
}
