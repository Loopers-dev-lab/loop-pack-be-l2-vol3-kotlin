package com.loopers.batch.ranking

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.ranking.weight")
data class RankingWeightProperties(
    val view: Double,
    val like: Double,
    val order: Double,
)
