package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingScoreConfig
import org.springframework.data.jpa.repository.JpaRepository

interface RankingScoreConfigJpaRepository : JpaRepository<RankingScoreConfig, Long>
