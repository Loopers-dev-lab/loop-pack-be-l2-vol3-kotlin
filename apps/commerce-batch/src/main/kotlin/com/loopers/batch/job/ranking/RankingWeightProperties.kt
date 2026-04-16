package com.loopers.batch.job.ranking

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ranking.weight")
data class RankingWeightProperties(
    val viewWeight: Double = 0.1,
    val likeWeight: Double = 0.2,
    val salesWeight: Double = 0.7,
)
